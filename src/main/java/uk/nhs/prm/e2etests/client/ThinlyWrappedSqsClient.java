package uk.nhs.prm.e2etests.client;

import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.model.Message;
import uk.nhs.prm.e2etests.client.AutoRefreshingRoleAssumingSqsClient;
import uk.nhs.prm.e2etests.model.SqsMessage;

import java.util.List;

@Component
public class ThinlyWrappedSqsClient {

    private AutoRefreshingRoleAssumingSqsClient sqsClient;

    public ThinlyWrappedSqsClient(
            AutoRefreshingRoleAssumingSqsClient sqsClient
    ) {
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

    public void postAMessage(String queueUrl, String message, String attributeKey, String attributeValue) {
        sqsClient.postAMessage(queueUrl, message, attributeKey, attributeValue);
    }
}
