package uk.nhs.prm.deduction.e2e.nems;

import uk.nhs.prm.deduction.e2e.queue.SqsQueue;

public class NemsEventMessageQueue {

    private SqsQueue sqsQueue;

    public NemsEventMessage readEventMessage() {
        return NemsEventMessage.parseMessage(sqsQueue.readMessageBody());
    }
}
