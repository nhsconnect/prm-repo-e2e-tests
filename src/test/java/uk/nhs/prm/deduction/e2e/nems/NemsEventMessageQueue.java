package uk.nhs.prm.deduction.e2e.nems;

import uk.nhs.prm.deduction.e2e.queue.SqsQueue;

public class NemsEventMessageQueue {

    private SqsQueue sqsQueue;
    private String queueUri;

    public NemsEventMessageQueue(SqsQueue sqsQueue, String queueUri) {
        this.sqsQueue = sqsQueue;
        this.queueUri = queueUri;
    }

    public NemsEventMessage readEventMessage() {
        String messageBody = sqsQueue.readMessageBody(queueUri);
        return NemsEventMessage.parseMessage(messageBody);
    }

}
