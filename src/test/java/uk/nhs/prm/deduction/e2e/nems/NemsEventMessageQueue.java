package uk.nhs.prm.deduction.e2e.nems;

import org.springframework.stereotype.Component;
import uk.nhs.prm.deduction.e2e.queue.SqsQueue;

@Component
public class NemsEventMessageQueue {

    private final SqsQueue sqsQueue;
    private final String queueUri;

    public NemsEventMessageQueue(SqsQueue sqsQueue, String queueUri) {
        this.sqsQueue = sqsQueue;
        this.queueUri = queueUri;
    }

    public NemsEventMessage readMessage() {
        log("** Reading message from the queue");

        log(String.format("** Queue Uri is %s", this.queueUri));
        String messageBody = sqsQueue.readMessageBody(this.queueUri);
        return NemsEventMessage.parseMessage(messageBody);
    }

    public void log(String messageBody) {
        System.out.println(messageBody);
    }
}
