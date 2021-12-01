package uk.nhs.prm.deduction.e2e.suspensions;

import org.json.JSONException;
import uk.nhs.prm.deduction.e2e.queue.SqsQueue;

public class SuspensionMessageQueue {
    protected final SqsQueue sqsQueue;
    protected final String queueUri;

    public SuspensionMessageQueue(SqsQueue sqsQueue, String queueUri) {
        this.sqsQueue = sqsQueue;
        this.queueUri = queueUri;
    }

    public SuspensionMessage readMessage() throws JSONException {
        log("** Reading message from the queue");

        log(String.format("** Queue Uri is %s", this.queueUri));
        String messageBody = sqsQueue.readMessageBody(this.queueUri);
        return SuspensionMessage.parseMessage(messageBody);
    }

    public void log(String messageBody) {
        System.out.println(messageBody);
    }
}
