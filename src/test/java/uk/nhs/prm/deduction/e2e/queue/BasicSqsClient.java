package uk.nhs.prm.deduction.e2e.queue;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.util.List;
import java.util.stream.Collectors;

@Component
@Lazy
public class BasicSqsClient implements TestSqsClient {
    private static final int MAX_VISIBILITY_TIMEOUT = 43200;

    private SqsClient sqsClient;

    public BasicSqsClient(SqsClient sqsClient) {
        this.sqsClient = sqsClient;
    }

    @Override
    public List<SqsMessage> readMessagesFrom(String queueUrl) {
        var receiveMessageRequest = ReceiveMessageRequest.builder()
                .visibilityTimeout(0)
                .queueUrl(queueUrl)
                .waitTimeSeconds(5)
                .maxNumberOfMessages(10)
                .messageAttributeNames("All")
                .attributeNames(QueueAttributeName.ALL)
                .build();

        return receiveMessages(receiveMessageRequest)
                .messages()
                .stream()
                .map(SqsMessage::new)
                .collect(Collectors.toList());
    }

    @Override
    public List<SqsMessage> readThroughMessages(String queueUrl, int visibilityTimeout) {
        int safeVisibilityTimeout = Math.min(visibilityTimeout, MAX_VISIBILITY_TIMEOUT);
        var receiveMessageRequest = ReceiveMessageRequest.builder()
            .visibilityTimeout(safeVisibilityTimeout)
            .queueUrl(queueUrl)
            .waitTimeSeconds(5)
            .maxNumberOfMessages(10)
            .messageAttributeNames("All")
            .attributeNames(QueueAttributeName.ALL)
            .build();

        return receiveMessages(receiveMessageRequest).messages()
                .stream()
                .map(SqsMessage::new)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteMessageFrom(String queueUrl, Message message) {
        sqsClient.deleteMessage(DeleteMessageRequest.builder().queueUrl(queueUrl).receiptHandle(message.receiptHandle()).build());
    }

    @Override
    public void deleteAllMessageFrom(String queueUrl) {
        sqsClient.purgeQueue(PurgeQueueRequest.builder().queueUrl(queueUrl).build());
    }
    public void postAMessage(String queueUrl, String message) {
        sqsClient.sendMessage(SendMessageRequest.builder().queueUrl(queueUrl).messageBody(message).build());
    }
    private ReceiveMessageResponse receiveMessages(ReceiveMessageRequest receiveMessageRequest) {
        try {
            return sqsClient.receiveMessage(receiveMessageRequest);
        }
        catch (Exception e) {
            throw new RuntimeException("Failure receiving messages from: " + receiveMessageRequest.queueUrl(), e);
        }
    }
}
