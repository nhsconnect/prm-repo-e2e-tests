package uk.nhs.prm.e2etests.client;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import uk.nhs.prm.e2etests.model.SqsMessage;

import java.util.HashMap;
import java.util.List;

public class BasicSqsClient implements TestSqsClient {
    private static final int MAX_VISIBILITY_TIMEOUT = 43200;

    private final SqsClient sqsClient;

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
                .toList();
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
                .toList();
    }

    @Override
    public void deleteMessageFrom(String queueUrl, Message message) {
        sqsClient.deleteMessage(DeleteMessageRequest.builder().queueUrl(queueUrl).receiptHandle(message.receiptHandle()).build());
    }

    @Override
    public void deleteAllMessagesFrom(String queueUrl) {
        sqsClient.purgeQueue(PurgeQueueRequest.builder().queueUrl(queueUrl).build());
    }

    public void postAMessage(String queueUrl, String message) {
        sqsClient.sendMessage(SendMessageRequest.builder().queueUrl(queueUrl).messageBody(message).build());
    }

    public void postAMessage(String queueUrl, String message, String attributeKey, String attributeValue) {
        var attributes = new HashMap<String, MessageAttributeValue>();
        attributes.put(attributeKey, getMessageAttributeValue(attributeValue));

        var messageRequest = SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody(message)
                .messageAttributes(attributes)
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
