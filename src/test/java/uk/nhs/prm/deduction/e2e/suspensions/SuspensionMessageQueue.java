package uk.nhs.prm.deduction.e2e.suspensions;

import org.json.JSONException;
import software.amazon.awssdk.services.sqs.model.Message;
import uk.nhs.prm.deduction.e2e.queue.SqsQueue;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;

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

    public boolean hasMessage(String messageBodyToCheck) {
        log(String.format("Checking if message is present on : %s",  this.queueUri));
        await().atMost(120, TimeUnit.SECONDS).with().pollInterval(2, TimeUnit.SECONDS).until(() -> messageIsOnQueue(messageBodyToCheck), equalTo(true));
        return true;
    }

    private boolean messageIsOnQueue(String messageBodyToCheck) {
        List<Message> allMessages = sqsQueue.readAllMessages(this.queueUri);
        if (!allMessages.isEmpty()) {
            for (Message message : allMessages) {
                if (message.body().contains(messageBodyToCheck)) {
                    return true;
                } else {
                    return false;
                }
            }
        }
        return false;
    }
}
