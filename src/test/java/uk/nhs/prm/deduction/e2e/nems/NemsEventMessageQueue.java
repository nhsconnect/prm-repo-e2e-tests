package uk.nhs.prm.deduction.e2e.nems;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.nhs.prm.deduction.e2e.TestConfiguration;
import uk.nhs.prm.deduction.e2e.queue.SqsQueue;

@Component
public class NemsEventMessageQueue {



    @Autowired
    private SqsQueue sqsQueue;


    public NemsEventMessage readEventMessage(String queueUri) {
        log("** Reading message from the queue");

        log(String.format("** Queue Uri is %s",queueUri));
        String messageBody = sqsQueue.readMessageBody(queueUri);
        return NemsEventMessage.parseMessage(messageBody);
    }


    public void log(String messageBody) {
        System.out.println(messageBody);
    }
}
