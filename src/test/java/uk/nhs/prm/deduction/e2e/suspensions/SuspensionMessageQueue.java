package uk.nhs.prm.deduction.e2e.suspensions;

import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.nhs.prm.deduction.e2e.queue.SqsQueue;

@Component
public class SuspensionMessageQueue {

    @Autowired
    private SqsQueue sqsQueue;

    public SuspensionMessage readEventMessage(String queueUri) throws JSONException {
        log("** Reading message from the queue");

        log(String.format("** Queue Uri is %s",queueUri));
        String messageBody = sqsQueue.readMessageBody(queueUri);
        return SuspensionMessage.parseMessage(messageBody);
    }


    public void log(String messageBody) {
        System.out.println(messageBody);
    }
}
