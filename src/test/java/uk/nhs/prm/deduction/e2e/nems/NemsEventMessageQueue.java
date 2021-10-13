package uk.nhs.prm.deduction.e2e.nems;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.nhs.prm.deduction.e2e.TestConfiguration;
import uk.nhs.prm.deduction.e2e.queue.SqsQueue;

@Component
public class NemsEventMessageQueue {


    @Autowired
    private TestConfiguration configuration ;
    @Autowired
    private SqsQueue sqsQueue;


    public NemsEventMessage readEventMessage() {
        log("** Reading message from the queue");

        log(String.format("** Queue Uri is %s",configuration.meshForwarderObservabilityQueueUri()));
        String messageBody = sqsQueue.readMessageBody(configuration.meshForwarderObservabilityQueueUri());
        return NemsEventMessage.parseMessage(messageBody);
    }


    public void log(String messageBody) {
        System.out.println(messageBody);
    }
}
