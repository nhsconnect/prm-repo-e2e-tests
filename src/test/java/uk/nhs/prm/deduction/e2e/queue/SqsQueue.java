package uk.nhs.prm.deduction.e2e.queue;

import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.model.Message;

import java.util.List;

@Component
public class SqsQueue {

    private SqsClient sqsClient;

    public SqsQueue() {
        this.sqsClient = new SqsClient();
    }

    public List<SqsMessage> readAllMessages(String queueUri) {
        return sqsClient.readAllMessageFrom(queueUri);
    }

    public List<SqsMessage> readAllWithVisbilityTimeout(String queueUri) {
        return sqsClient.readMessageWithVisibilityTimeoutFrom(queueUri);
    }

    public void deleteMessage(String queueUri,Message message) {
        sqsClient.deleteMessageFrom(queueUri,message);
    }
    public void deleteAllMessage(String queueUri) {
        sqsClient.deleteAllMessageFrom(queueUri);
    }
}
