package uk.nhs.prm.deduction.e2e.queue;

import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.model.Message;

import java.util.List;

@Component
public class SqsQueue {

    private BasicSqsClient sqsClient;

    public SqsQueue(BasicSqsClient sqsClient) {
        this.sqsClient = sqsClient;
    }

    public List<SqsMessage> readMessagesFrom(String queueUri) {
        return sqsClient.readMessagesFrom(queueUri);
    }

    public List<SqsMessage> readThroughMessages(String queueUri, int visibilityTimeout) {
        return sqsClient.readThroughMessages(queueUri, visibilityTimeout);
    }

    public void deleteMessage(String queueUri,Message message) {
        sqsClient.deleteMessageFrom(queueUri,message);
    }
    public void deleteAllMessage(String queueUri) {
        sqsClient.deleteAllMessageFrom(queueUri);
    }
}
