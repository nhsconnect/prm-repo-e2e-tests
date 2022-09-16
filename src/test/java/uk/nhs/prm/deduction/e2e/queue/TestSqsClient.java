package uk.nhs.prm.deduction.e2e.queue;

import software.amazon.awssdk.services.sqs.model.Message;

import java.util.List;

public interface TestSqsClient {
    List<SqsMessage> readMessagesFrom(String queueUrl);

    List<SqsMessage> readThroughMessages(String queueUrl, int visibilityTimeout);

    void deleteMessageFrom(String queueUrl, Message message);

    void deleteAllMessagesFrom(String queueUrl);

    void postAMessage(String queueUrl, String message);
}
