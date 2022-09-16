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

    public void deleteMessage(String queueUrl, Message message) {
        sqsClient.deleteMessageFrom(queueUrl, message);
    }

    public void deleteAllMessages(String queueUri) {
        sqsClient.deleteAllMessagesFrom(queueUri);
    }

    public void postAMessage(String queueUrl, String message){
        sqsClient.postAMessage(queueUrl, message);
    }
}
