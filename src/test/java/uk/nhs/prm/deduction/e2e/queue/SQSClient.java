package uk.nhs.prm.deduction.e2e.queue;

import org.awaitility.core.ThrowingRunnable;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class SQSClient {
    SqsClient sqsClient = SqsClient.create();


    public List<Message> readAllMessageFrom(String queueUrl) {

        ReceiveMessageRequest receiveMessageRequest = ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .waitTimeSeconds(5)
                .maxNumberOfMessages(1)
                .build();
        List<Message> messages = new ArrayList<>();

        messages =messagesReadMessageUntilQueueIsEmpty(receiveMessageRequest);

        if (messages.isEmpty()) {
            log("** No messages found on the queue");
            throw new AssertionError("No messages found on the queue: " + queueUrl);
        }
        log("** Read message from queue");
        return messages;
    }

    private List<Message> messagesReadMessageUntilQueueIsEmpty(ReceiveMessageRequest receiveMessageRequest) {
        List<Message> messages = new ArrayList<>();
        List<Message> messageList = sqsClient.receiveMessage(receiveMessageRequest).messages();
        int retry = 1;
        try {
            while (!messageList.isEmpty() || retry != 0) {

                if (!messageList.isEmpty()) {
                    messages.add(messageList.get(0));
                } else {
                    Thread.sleep(2000);
                    retry = retry - 1;
                }

                messageList = sqsClient.receiveMessage(receiveMessageRequest).messages();
            }
        }catch (Exception e){
            throw new RuntimeException(e.getMessage());
        }



        return messages;
    }



    public void deleteMessageFrom(String queueUrl, Message message) {
        sqsClient.deleteMessage(DeleteMessageRequest.builder().queueUrl(queueUrl).receiptHandle(message.receiptHandle()).build());

    }

    public void deleteAllMessageFrom(String queueUrl) {
        sqsClient.purgeQueue(PurgeQueueRequest.builder().queueUrl(queueUrl).build());

    }

    public void log(String messageBody) {
        System.out.println(messageBody);
    }
}
