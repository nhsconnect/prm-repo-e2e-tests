package uk.nhs.prm.deduction.e2e.queue;

import lombok.extern.slf4j.Slf4j;
import org.awaitility.core.ConditionTimeoutException;
import org.json.JSONException;
import uk.nhs.prm.deduction.e2e.models.ResolutionMessage;
import uk.nhs.prm.deduction.e2e.utility.QueueHelper;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.hamcrest.Matchers.*;

@Slf4j
public class QueueMessageHelper {
    protected final ThinlyWrappedSqsClient thinlyWrappedSqsClient;
    protected final String queueUri;

    public QueueMessageHelper(ThinlyWrappedSqsClient thinlyWrappedSqsClient, String queueUri) {
        this.thinlyWrappedSqsClient = thinlyWrappedSqsClient;
        this.queueUri = queueUri;
    }

    public void deleteAllMessages() {
        log(String.format("Trying to delete all the messages on : %s", this.queueUri));
        try {
            thinlyWrappedSqsClient.deleteAllMessages(queueUri);
        } catch (Exception e) {
            log.warn("Error encountered while deleting the messages on the queue : " + queueUri, e);
        }
    }

    public void deleteMessage(SqsMessage sqsMessage) {
        thinlyWrappedSqsClient.deleteMessage(queueUri, sqsMessage.getMessage());
    }

    public void log(String messageBody) {
        System.out.println(messageBody);
    }

    public SqsMessage getMessageContaining(String substring) {
        log(String.format("Checking if message is present on : %s", this.queueUri));
        SqsMessage found = await().atMost(120, TimeUnit.SECONDS)
                .with()
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .until(() -> findMessageContaining(substring), notNullValue());
        log(String.format("Found message on : %s", this.queueUri));
        return found;
    }

    public boolean verifyNoMessageContaining(String substring) {
        try {
            await().atMost(60, TimeUnit.SECONDS)
                    .with()
                    .pollInterval(100, TimeUnit.MILLISECONDS)
                    .until(() -> findMessageContaining(substring), notNullValue());
            log(String.format("A message on %s match the given substring %s. Returning false", this.queueUri, substring));
            return false;
        } catch (ConditionTimeoutException error) {
            log(String.format("Confirmed no message on %s match the given substring %s.", this.queueUri, substring));
            return true;
        }
    }

    public List<SqsMessage> getAllMessageContaining(String substring) {
        log(String.format("Checking if message is present on : %s", this.queueUri));
        List<SqsMessage> allMessages = new ArrayList<>();
        await().atMost(120, TimeUnit.SECONDS)
                .with()
                .pollInterval(100, TimeUnit.MILLISECONDS).untilAsserted(() -> {
                    SqsMessage found = findMessageContaining(substring);
                    if (found != null) {
                        allMessages.add(found);
                    }
                    assertTrue(allMessages.size() >= 2);
                });

        log(String.format("Found message on : %s", this.queueUri));
        return allMessages;
    }

    public List<SqsMessage> getAllMessageContaining(String substring, int expectedNumberOfMessages, long secondsToPoll) {
        // Because of the invisibility property of sqs, it is difficult to get all existing messages in one go.
        // This method attempts to poll the queue repeatedly for x seconds, until we got n messages.
        // If we couldn't get all n messages, will just return with what we already got.
        log(String.format("Try to get at least %d messages with substring %s on queue", expectedNumberOfMessages, substring));
        List<SqsMessage> allMessages = new ArrayList<>();

        try {
            await().atMost(secondsToPoll, TimeUnit.SECONDS)
                    .with()
                    .pollInterval(100, TimeUnit.MILLISECONDS).untilAsserted(() -> {
                        SqsMessage found = findMessageContaining(substring);
                        if (found != null) {
                            allMessages.add(found);
                        }
                        assertTrue(allMessages.size() >= expectedNumberOfMessages);
                    });
        } catch (ConditionTimeoutException error) {
            log(String.format("Could not get %d messages from the queue matching the substring %s. Just return what we got", expectedNumberOfMessages, substring));
        }
        return allMessages;
    }

    public SqsMessage getMessageContainingAttribute(String attribute, String expectedValue) {
        return getMessageContainingAttribute(attribute, expectedValue, 120, TimeUnit.SECONDS);
    }

    public SqsMessage getMessageContainingAttribute(String attribute, String expectedValue, int timeout, TimeUnit timeUnit) {
        log(String.format("Checking if message is present on : %s", this.queueUri));
        return await().atMost(timeout, timeUnit)
                .with()
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .until(() -> findMessageWithAttribute(attribute, expectedValue), notNullValue());
    }

    public boolean hasResolutionMessage(ResolutionMessage resolutionMessage) {
        log(String.format("Checking if message is present on : %s", this.queueUri));
        await().atMost(120, TimeUnit.SECONDS)
                .with()
                .pollInterval(2, TimeUnit.SECONDS)
                .until(() -> hasResolutionMessageNow(resolutionMessage), equalTo(true));
        return true;
    }

    public List<SqsMessage> getNextMessages(LocalDateTime timeoutAt) {
        log(String.format("Checking for messages on : %s", this.queueUri));
        int pollInterval = 5;
        long timeoutSeconds = Math.max(LocalDateTime.now().until(timeoutAt, ChronoUnit.SECONDS), pollInterval + 1);
        return await().atMost(timeoutSeconds, TimeUnit.SECONDS)
                .with()
                .pollInterval(pollInterval, TimeUnit.SECONDS)
                .until(() -> findMessagesOnQueue((int) timeoutSeconds), notNullValue());
    }

    private List<SqsMessage> findMessagesOnQueue(int visibilityTimeout) {
        List<SqsMessage> messages = thinlyWrappedSqsClient.readThroughMessages(this.queueUri, visibilityTimeout);
        return messages.isEmpty() ? null : messages;
    }

    private SqsMessage findMessageContaining(String substring) {
        List<SqsMessage> allMessages = thinlyWrappedSqsClient.readThroughMessages(this.queueUri, 180);
        for (SqsMessage message : allMessages) {
            log(String.format("Finding message with substring %s on queue: %s", substring, this.queueUri));
            if (message.contains(substring)) {
                return message;
            }
        }
        return null;
    }

    public SqsMessage findMessageWithAttribute(String attribute, String expectedValue) {
        List<SqsMessage> allMessages = thinlyWrappedSqsClient.readThroughMessages(this.queueUri, 180);
        for (SqsMessage message : allMessages) {
            System.out.println("just finding message, checking attribute : " + attribute + " expected value is : " + expectedValue);
            if (message.attributes().get(attribute).stringValue().equals(expectedValue)) {
                return message;
            }
        }
        return null;
    }

    public SqsMessage getMessageContainingForTechnicalTestRun(String substring) {
        log(String.format("Checking if message is present on : %s", this.queueUri));
        return await().atMost(1800, TimeUnit.SECONDS)
                .with()
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .until(() -> findMessageContainingWitHigherVisibilityTimeOut(substring), notNullValue());
    }

    private SqsMessage findMessageContainingWitHigherVisibilityTimeOut(String substring) {
        List<SqsMessage> allMessages = thinlyWrappedSqsClient.readThroughMessages(this.queueUri, 600);
        for (SqsMessage message : allMessages) {
            System.out.println("just finding message, checking conversationId: " + substring);
            if (message.contains(substring)) {
                return message;
            }
        }
        return null;
    }

    private boolean hasResolutionMessageNow(ResolutionMessage messageToCheck) throws JSONException {
        List<SqsMessage> allMessages = thinlyWrappedSqsClient.readMessagesFrom(this.queueUri);
        for (SqsMessage message : allMessages) {
            ResolutionMessage resolutionMessage = QueueHelper.getNonSensitiveDataMessage(message);
            if (QueueHelper.checkIfMessageIsExpectedMessage(resolutionMessage, messageToCheck)) {
                return true;
            }
        }
        return false;
    }

    protected void postAMessageWithAttribute(String message, String attributeKey, String attributeValue) {
        thinlyWrappedSqsClient.postAMessage(queueUri, message, attributeKey, attributeValue);
    }
}
