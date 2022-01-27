package uk.nhs.prm.deduction.e2e.nems;

import org.springframework.stereotype.Component;
import uk.nhs.prm.deduction.e2e.queue.SqsMessage;
import uk.nhs.prm.deduction.e2e.queue.SqsQueue;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;

@Component
public class NemsEventMessageQueue {

    private final SqsQueue sqsQueue;
    private final String queueUri;

    public NemsEventMessageQueue(SqsQueue sqsQueue, String queueUri) {
        this.sqsQueue = sqsQueue;
        this.queueUri = queueUri;
    }

    public boolean hasMessage(String messageBodyToCheck) {
        log(String.format("Checking if message is present on : %s",  this.queueUri));
        await().atMost(120, TimeUnit.SECONDS).with().pollInterval(2, TimeUnit.SECONDS).until(() -> messageIsOnQueue(messageBodyToCheck), equalTo(true));
        log("Message is present on queue");
        return true;
    }

    private boolean messageIsOnQueue(String messageBodyToCheck) {
        List<SqsMessage> allMessages = sqsQueue.readAllMessages(this.queueUri);
        if (!allMessages.isEmpty()) {
            for (var message : allMessages) {
                if (message.contains(messageBodyToCheck)) {
                    return true;
                } else {
                    return false;
                }
            }
        } 
        return false;
    }

    public void deleteAllMessages() {
        sqsQueue.deleteAllMessage(queueUri);
    }

    public void log(String messageBody) {
        System.out.println(messageBody);
    }
}
