package uk.nhs.prm.deduction.e2e.queue;

import org.json.JSONException;
import uk.nhs.prm.deduction.e2e.models.ResolutionMessage;
import uk.nhs.prm.deduction.e2e.utility.QueueHelper;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

public class QueueMessageHelper {
    protected final SqsQueue sqsQueue;
    protected final String queueUri;

    public QueueMessageHelper(SqsQueue sqsQueue, String queueUri) {
        this.sqsQueue = sqsQueue;
        this.queueUri = queueUri;
    }

    public void deleteAllMessages() {
        sqsQueue.deleteAllMessage(queueUri);
    }

    public void log(String messageBody) {
        System.out.println(messageBody);
    }

    public SqsMessage getMessageContaining(String substring) {
        log(String.format("Checking if message is present on : %s", this.queueUri));
        return await().atMost(120, TimeUnit.SECONDS)
                .with()
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .until(() -> findMessageContaining(substring), notNullValue());
    }
    public SqsMessage getMessageContainingAttribute(String attribute,String expectedValue) {
        log(String.format("Checking if message is present on : %s", this.queueUri));
        return await().atMost(120, TimeUnit.SECONDS)
                .with()
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .until(() -> findMessageWithAttribute(attribute,expectedValue), notNullValue());
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
        var timeoutSeconds = Math.max(LocalDateTime.now().until(timeoutAt, ChronoUnit.SECONDS), pollInterval + 1);
        return await().atMost(timeoutSeconds, TimeUnit.SECONDS)
                .with()
                .pollInterval(pollInterval, TimeUnit.SECONDS)
                .until(() -> findMessagesOnQueue((int) timeoutSeconds), notNullValue());
    }
    private List<SqsMessage> findMessagesOnQueue(int visibilityTimeout) {
        List<SqsMessage> messages = sqsQueue.readThroughMessages(this.queueUri, visibilityTimeout);
        return messages.isEmpty() ? null : messages;
    }

    private SqsMessage findMessageContaining(String substring) {
        var allMessages = sqsQueue.readThroughMessages(this.queueUri, 180);
        for (var message : allMessages) {
            System.out.println("just finding message, checking: " + message.id());
            if (message.contains(substring)) {
                return message;
            }
        }
        return null;
    }

    public SqsMessage findMessageWithAttribute(String attribute, String expectedValue) {
        var allMessages = sqsQueue.readThroughMessages(this.queueUri, 180);
        for (var message : allMessages) {
            System.out.println("just finding message, checking: " + message.id());
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
        var allMessages = sqsQueue.readThroughMessages(this.queueUri, 600);
        for (var message : allMessages) {
            System.out.println("just finding message, checking: " + message.id());
            if (message.contains(substring)) {
                return message;
            }
        }
        return null;
    }


    private boolean hasResolutionMessageNow(ResolutionMessage messageToCheck) throws JSONException {
        List<SqsMessage> allMessages = sqsQueue.readMessagesFrom(this.queueUri);
        for (var message : allMessages) {
            ResolutionMessage resolutionMessage = QueueHelper.getNonSensitiveDataMessage(message);
            if (QueueHelper.checkIfMessageIsExpectedMessage(resolutionMessage, messageToCheck)) {
                return true;
            }
        }
        return false;
    }

    public void postAMessage(String message) {
        sqsQueue.postAMessage(queueUri,message);
    }
}
