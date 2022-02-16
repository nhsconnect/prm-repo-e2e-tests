package uk.nhs.prm.deduction.e2e.queue;

import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.model.Message;
import uk.nhs.prm.deduction.e2e.performance.awsauth.AutoRefreshingRoleAssumingSqsClient;

import java.util.List;

@Component
public class SqsQueue {

    private AutoRefreshingRoleAssumingSqsClient sqsClient;

    public SqsQueue(AutoRefreshingRoleAssumingSqsClient sqsClient) {
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
