package uk.nhs.prm.e2etests.nems;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.nhs.prm.e2etests.model.NemsEventMessage;
import uk.nhs.prm.e2etests.queue.SqsMessage;
import uk.nhs.prm.e2etests.queue.ThinlyWrappedSqsClient;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;

@Component
public class NemsEventMessageQueue {
    private final Logger LOGGER = LogManager.getLogger(NemsEventMessage.class);
    private final ThinlyWrappedSqsClient thinlyWrappedSqsClient;
    private final String queueUri;

    @Autowired
    public NemsEventMessageQueue(ThinlyWrappedSqsClient thinlyWrappedSqsClient, String queueUri) {
        this.thinlyWrappedSqsClient = thinlyWrappedSqsClient;
        this.queueUri = queueUri;
    }

    public boolean hasMessage(String messageBodyToCheck) {
        LOGGER.info("Checking if message is present on: {}",  this.queueUri);
        await().atMost(120, TimeUnit.SECONDS).with().pollInterval(2, TimeUnit.SECONDS).until(() -> messageIsOnQueue(messageBodyToCheck), equalTo(true));
        LOGGER.info("The message is present on the queue.");
        return true;
    }

    private boolean messageIsOnQueue(String messageBodyToCheck) {
        List<SqsMessage> allMessages = thinlyWrappedSqsClient.readMessagesFrom(this.queueUri);
        if (!allMessages.isEmpty()) {
            for (var message : allMessages) {
                return message.contains(messageBodyToCheck);
            }
        } 
        return false;
    }

    public void deleteAllMessages() {
        thinlyWrappedSqsClient.deleteAllMessages(queueUri);
    }
}
