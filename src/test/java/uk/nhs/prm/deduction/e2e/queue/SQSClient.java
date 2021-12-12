package uk.nhs.prm.deduction.e2e.queue;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.util.List;


public class SQSClient {
    SqsClient sqsClient = SqsClient.create();
    public List<Message> readMessageFrom(String queueUrl) {

        ReceiveMessageRequest receiveMessageRequest = ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .build();

        List<Message> messages = sqsClient.receiveMessage(receiveMessageRequest).messages();

        if (messages.isEmpty()) {
            log("** No messages found on the queue");
            throw new AssertionError("No messages found on the queue: " +  queueUrl);
        }
        log("** Read messages from queue count : "+messages.size());
        return messages;//messages.get(0).body();
    }

    public void deleteMessageFrom(String queueUrl, Message message) {
        sqsClient.deleteMessage(DeleteMessageRequest.builder().queueUrl(queueUrl).receiptHandle(message.receiptHandle()).build());

    }

    public void log(String messageBody) {
        System.out.println(messageBody);
    }
}
