package uk.nhs.prm.deduction.e2e.suspensions;

import uk.nhs.prm.deduction.e2e.queue.SqsMessage;
import uk.nhs.prm.deduction.e2e.queue.SqsQueue;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

public class SuspensionMessageQueue {
    protected final SqsQueue sqsQueue;
    protected final String queueUri;

    public SuspensionMessageQueue(SqsQueue sqsQueue, String queueUri) {
        this.sqsQueue = sqsQueue;
        this.queueUri = queueUri;
    }
    public void deleteAllMessages(){
        sqsQueue.deleteAllMessage(queueUri);
    }

    public void log(String messageBody) {
        System.out.println(messageBody);
    }

    public SqsMessage getMessageContaining(String substring) {
        log(String.format("Checking if message is present on : %s",  this.queueUri));
        return await().atMost(120, TimeUnit.SECONDS)
                .with()
                .pollInterval(2, TimeUnit.SECONDS)
                .until(() -> findMessageContaining(substring), notNullValue());
    }

    public boolean hasMessageContaining(String substring) {
        log(String.format("Checking if message is present on : %s",  this.queueUri));
        await().atMost(120, TimeUnit.SECONDS)
                .with()
                .pollInterval(2, TimeUnit.SECONDS)
                .until(() -> messageIsOnQueue(substring), equalTo(true));
        return true;
    }

    public List<SqsMessage> getNextMessages() {
        log(String.format("Checking for messages on : %s",  this.queueUri));
        return await().atMost(30, TimeUnit.SECONDS)
            .with()
            .pollInterval(1, TimeUnit.SECONDS)
            .until(this::findMessagesOnQueue, notNullValue());
    }

    private List<SqsMessage> findMessagesOnQueue() {
        List<SqsMessage> messages = sqsQueue.readThroughMessages(this.queueUri);
        return messages.isEmpty() ? null : messages;
    }

    private SqsMessage findMessageContaining(String substring) {
        var allMessages = sqsQueue.readThroughMessages(this.queueUri);
        if (!allMessages.isEmpty()) {
            for (var message : allMessages) {
                System.out.println("just finding message, checking: " + message.id());
                if (message.contains(substring)) {
                    return message;
                }
            }
        }
        return null;
    }

    private boolean messageIsOnQueue(String messageBodyToCheck) {
        List<SqsMessage> allMessages = sqsQueue.readMessagesFrom(this.queueUri);
        if (!allMessages.isEmpty()) {
            for (var message : allMessages) {
                if (message.contains(messageBodyToCheck)) {
                    return true;
                }
            }
        }
        return false;
    }
}
