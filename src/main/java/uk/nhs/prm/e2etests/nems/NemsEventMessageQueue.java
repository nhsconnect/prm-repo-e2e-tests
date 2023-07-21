package uk.nhs.prm.e2etests.nems;

import uk.nhs.prm.e2etests.queue.ThinlyWrappedSqsClient;
import lombok.extern.log4j.Log4j2;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;

@Log4j2
public abstract class NemsEventMessageQueue {
    private final ThinlyWrappedSqsClient thinlyWrappedSqsClient;
    private final String queueUri;

    protected NemsEventMessageQueue(
            ThinlyWrappedSqsClient thinlyWrappedSqsClient,
            String queueUri
    ) {
        this.thinlyWrappedSqsClient = thinlyWrappedSqsClient;
        this.queueUri = queueUri;
    }

    public boolean hasMessage(String messageBodyToCheck) {
        log.info("Checking if message is present on: {}",  this.queueUri);
        await().atMost(120, TimeUnit.SECONDS).with().pollInterval(2, TimeUnit.SECONDS).until(() -> messageIsOnQueue(messageBodyToCheck), equalTo(true));
        log.info("The message is present on the queue.");
        return true;
    }

    private boolean messageIsOnQueue(String messageBodyToCheck) {
        return thinlyWrappedSqsClient.readMessagesFrom(this.queueUri)
                .stream()
                .anyMatch(message -> message.contains(messageBodyToCheck));
    }

    public void deleteAllMessages() {
        thinlyWrappedSqsClient.deleteAllMessages(queueUri);
    }
}
