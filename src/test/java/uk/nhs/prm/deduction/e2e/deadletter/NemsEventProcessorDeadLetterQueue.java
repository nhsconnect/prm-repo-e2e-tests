package uk.nhs.prm.deduction.e2e.deadletter;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.nhs.prm.deduction.e2e.TestConfiguration;
import uk.nhs.prm.deduction.e2e.nems.NemsEventMessageQueue;
import uk.nhs.prm.deduction.e2e.queue.SqsQueue;
@Component
public class NemsEventProcessorDeadLetterQueue extends NemsEventMessageQueue {

    @Autowired
    public NemsEventProcessorDeadLetterQueue(SqsQueue sqsQueue, TestConfiguration configuration) {
        super(sqsQueue, configuration.nemsEventProcessorDeadLetterQueue());
    }
}
