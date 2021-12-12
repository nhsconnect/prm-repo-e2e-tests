package uk.nhs.prm.deduction.e2e.queue;

import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.model.Message;

import java.util.List;

@Component
public class SqsQueue {

    private SQSClient sqsClient;

    public SqsQueue() {
        this.sqsClient = new SQSClient();
    }

    public List<Message> readMessageBody(String queueUri) {
        return sqsClient.readMessageFrom(queueUri);
    }

    public void deleteMessage(String queueUri,Message message) {
        sqsClient.deleteMessageFrom(queueUri,message);
    }
}
