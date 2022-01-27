package uk.nhs.prm.deduction.e2e.suspensions;

import software.amazon.awssdk.services.sqs.model.Message;
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

    private SqsMessage findMessageContaining(String substring) {
        var allMessages = sqsQueue.readAllMessages(this.queueUri);
        if (!allMessages.isEmpty()) {
            for (var message : allMessages) {
                if (message.body.contains(substring)) {
                    return message;
                } else {
                    return null;
                }
            }
        }
        return null;
    }

    private boolean messageIsOnQueue(String messageBodyToCheck) {
        List<SqsMessage> allMessages = sqsQueue.readAllMessages(this.queueUri);
        if (!allMessages.isEmpty()) {
            for (var message : allMessages) {
                if (message.body.contains(messageBodyToCheck)) {
                    return true;
                } else {
                    return false;
                }
            }
        }
        return false;
    }
}
