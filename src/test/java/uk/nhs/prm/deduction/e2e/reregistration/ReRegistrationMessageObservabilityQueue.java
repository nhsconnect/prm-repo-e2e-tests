package uk.nhs.prm.deduction.e2e.reregistration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.nhs.prm.deduction.e2e.TestConfiguration;
import uk.nhs.prm.deduction.e2e.nems.NemsEventMessageQueue;
import uk.nhs.prm.deduction.e2e.queue.SqsQueue;

@Component
public class ReRegistrationMessageObservabilityQueue extends NemsEventMessageQueue {

    @Autowired
    public ReRegistrationMessageObservabilityQueue(SqsQueue sqsQueue, TestConfiguration configuration) {
        super(sqsQueue, configuration.reRegistrationObservabilityQueueUri());
    }

}
