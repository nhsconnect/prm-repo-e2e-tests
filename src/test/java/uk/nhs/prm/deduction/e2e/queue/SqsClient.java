package uk.nhs.prm.deduction.e2e.queue;

import software.amazon.awssdk.services.sqs.model.*;
import java.util.List;
import java.util.stream.Collectors;

public class SqsClient {
    software.amazon.awssdk.services.sqs.SqsClient sqsClient = software.amazon.awssdk.services.sqs.SqsClient.create();

    public List<SqsMessage> readAllMessageFrom(String queueUrl) {
        var receiveMessageRequest = ReceiveMessageRequest.builder()
                .visibilityTimeout(0)
                .queueUrl(queueUrl)
                .waitTimeSeconds(5)
                .maxNumberOfMessages(10)
                .attributeNames(QueueAttributeName.ALL)
                .build();
        var messages = sqsClient.receiveMessage(receiveMessageRequest)
                .messages()
                .stream()
                .map(m -> new SqsMessage(m))
                .collect(Collectors.toList());

        return messages;
    }

    public List<SqsMessage> readMessageWithVisibilityTimeoutFrom(String queueUrl) {
        var receiveMessageRequest = ReceiveMessageRequest.builder()
            .visibilityTimeout(180)
            .queueUrl(queueUrl)
            .waitTimeSeconds(5)
            .maxNumberOfMessages(10)
            .attributeNames(QueueAttributeName.ALL)
            .build();

        return sqsClient.receiveMessage(receiveMessageRequest)
            .messages()
            .stream()
            .map(m -> new SqsMessage(m))
            .collect(Collectors.toList());
    }

    public void deleteMessageFrom(String queueUrl, Message message) {
        sqsClient.deleteMessage(DeleteMessageRequest.builder().queueUrl(queueUrl).receiptHandle(message.receiptHandle()).build());
    }

    public void deleteAllMessageFrom(String queueUrl) {
        sqsClient.purgeQueue(PurgeQueueRequest.builder().queueUrl(queueUrl).build());
    }
}
