package uk.nhs.prm.e2etests.service;

import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest;
import org.springframework.beans.factory.annotation.Autowired;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.SqsClient;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.regions.Region;
import uk.nhs.prm.e2etests.model.SqsMessage;

import java.util.stream.Collectors;
import java.util.List;
import java.util.Map;

@Service
public class SqsService {
    private static final int MAX_VISIBILITY_TIMEOUT = 43200;

    private final SqsClient sqsClient;

    @Autowired
    public SqsService(AwsCredentialsProvider awsCredentialsProvider) {
        this.sqsClient = SqsClient.builder()
                .region(Region.EU_WEST_2)
                .credentialsProvider(awsCredentialsProvider)
                .build();
    }

    public List<SqsMessage> readMessagesFrom(String queueUrl) {
        ReceiveMessageRequest receiveMessageRequest = ReceiveMessageRequest.builder()
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
                .toList();
    }

    public List<SqsMessage> readThroughMessages(String queueUrl, int visibilityTimeout) {
        int safeVisibilityTimeout = Math.min(visibilityTimeout, MAX_VISIBILITY_TIMEOUT);

        ReceiveMessageRequest receiveMessageRequest = ReceiveMessageRequest.builder()
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
                .toList();
    }

    public void deleteMessageFrom(String queueUrl, Message message) {
        sqsClient.deleteMessage(DeleteMessageRequest.builder().queueUrl(queueUrl).receiptHandle(message.receiptHandle()).build());
    }

    public void deleteAllMessagesFrom(String queueUrl) {
        sqsClient.purgeQueue(PurgeQueueRequest.builder().queueUrl(queueUrl).build());
    }

    public void postAMessage(String queueUrl, String message) {
        sqsClient.sendMessage(SendMessageRequest.builder().queueUrl(queueUrl).messageBody(message).build());
    }

    public void postAMessage(String queueUrl, String message, Map<String, String> attributes) {
        final Map<String, MessageAttributeValue> messageAttributes = attributes.entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        element -> getMessageAttributeValue(element.getValue())
                ));

        final SendMessageRequest messageRequest = SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody(message)
                .messageAttributes(messageAttributes)
                .build();

        sqsClient.sendMessage(messageRequest);
    }

    private ReceiveMessageResponse receiveMessages(ReceiveMessageRequest receiveMessageRequest) {
        try {
            return sqsClient.receiveMessage(receiveMessageRequest);
        }
        catch (Exception e) {
            throw new RuntimeException("Failure receiving messages from: " + receiveMessageRequest.queueUrl(), e);
        }
    }

    private MessageAttributeValue getMessageAttributeValue(String attributeValue) {
        return MessageAttributeValue.builder()
                .dataType("String")
                .stringValue(attributeValue)
                .build();
    }
}
