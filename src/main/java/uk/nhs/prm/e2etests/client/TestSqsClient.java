package uk.nhs.prm.e2etests.client;

import software.amazon.awssdk.services.sqs.model.Message;
import uk.nhs.prm.e2etests.model.SqsMessage;

import java.util.List;

public interface TestSqsClient {
    List<SqsMessage> readMessagesFrom(String queueUrl);

    List<SqsMessage> readThroughMessages(String queueUrl, int visibilityTimeout);

    void deleteMessageFrom(String queueUrl, Message message);

    void deleteAllMessagesFrom(String queueUrl);

    void postAMessage(String queueUrl, String message);

    void postAMessage(String queueUrl, String message, String attributeKey, String attributeMessage);
}
