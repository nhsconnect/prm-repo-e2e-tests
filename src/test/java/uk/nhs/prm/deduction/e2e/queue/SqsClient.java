package uk.nhs.prm.deduction.e2e.queue;

import software.amazon.awssdk.services.sqs.model.*;
import uk.nhs.prm.deduction.e2e.performance.AssumeRoleAuthAutoRefreshSqsClientFactory;

import java.util.List;
import java.util.stream.Collectors;

public class SqsClient {
    software.amazon.awssdk.services.sqs.SqsClient sqsClient = software.amazon.awssdk.services.sqs.SqsClient.create();

    private final AssumeRoleAuthAutoRefreshSqsClientFactory autoRefreshClientFactory = new AssumeRoleAuthAutoRefreshSqsClientFactory();

    public List<SqsMessage> readMessagesFrom(String queueUrl) {
        var receiveMessageRequest = ReceiveMessageRequest.builder()
                .visibilityTimeout(0)
                .queueUrl(queueUrl)
                .waitTimeSeconds(5)
                .maxNumberOfMessages(10)
                .attributeNames(QueueAttributeName.ALL)
                .build();

        return receiveMessages(receiveMessageRequest)
                .messages()
                .stream()
                .map(SqsMessage::new)
                .collect(Collectors.toList());
    }

    public List<SqsMessage> readThroughMessages(String queueUrl, int visibilityTimeout) {
        var receiveMessageRequest = ReceiveMessageRequest.builder()
            .visibilityTimeout(visibilityTimeout)
            .queueUrl(queueUrl)
            .waitTimeSeconds(5)
            .maxNumberOfMessages(10)
            .attributeNames(QueueAttributeName.ALL)
            .build();

        return receiveMessages(receiveMessageRequest).messages()
                .stream()
                .map(SqsMessage::new)
                .collect(Collectors.toList());
    }

    public void deleteMessageFrom(String queueUrl, Message message) {
        sqsClient.deleteMessage(DeleteMessageRequest.builder().queueUrl(queueUrl).receiptHandle(message.receiptHandle()).build());
    }

    public void deleteAllMessageFrom(String queueUrl) {
        sqsClient.purgeQueue(PurgeQueueRequest.builder().queueUrl(queueUrl).build());
    }

    private ReceiveMessageResponse receiveMessages(ReceiveMessageRequest receiveMessageRequest) {
        try {
            return sqsClient.receiveMessage(receiveMessageRequest);
        }
        catch (SqsException e) {
            System.out.println("Updating client to use assume role refresh request");
            sqsClient = autoRefreshClientFactory.createAssumeRoleAutoRefreshSqsClient();
            System.out.println("Attempting receive messages with updated assume role credentials provider");
            return sqsClient.receiveMessage(receiveMessageRequest);
        }
    }

}
