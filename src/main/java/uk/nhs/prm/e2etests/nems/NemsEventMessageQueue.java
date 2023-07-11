package uk.nhs.prm.e2etests.nems;

import org.springframework.stereotype.Component;
import uk.nhs.prm.deduction.e2e.queue.SqsMessage;
import uk.nhs.prm.deduction.e2e.queue.ThinlyWrappedSqsClient;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;

@Component
public class NemsEventMessageQueue {

    private final ThinlyWrappedSqsClient thinlyWrappedSqsClient;
    private final String queueUri;

    public NemsEventMessageQueue(ThinlyWrappedSqsClient thinlyWrappedSqsClient, String queueUri) {
        this.thinlyWrappedSqsClient = thinlyWrappedSqsClient;
        this.queueUri = queueUri;
    }

    public boolean hasMessage(String messageBodyToCheck) {
        log(String.format("Checking if message is present on : %s",  this.queueUri));
        await().atMost(120, TimeUnit.SECONDS).with().pollInterval(2, TimeUnit.SECONDS).until(() -> messageIsOnQueue(messageBodyToCheck), equalTo(true));
        log("Message is present on queue");
        return true;
    }

    private boolean messageIsOnQueue(String messageBodyToCheck) {
        List<SqsMessage> allMessages = thinlyWrappedSqsClient.readMessagesFrom(this.queueUri);
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
        thinlyWrappedSqsClient.deleteAllMessages(queueUri);
    }

    public void log(String messageBody) {
        System.out.println(messageBody);
    }
}
