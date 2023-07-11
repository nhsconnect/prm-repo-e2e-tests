package uk.nhs.prm.deduction.e2e.deadletter;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.nhs.prm.deduction.e2e.TestConfiguration;
import uk.nhs.prm.e2etests.nems.NemsEventMessageQueue;
import uk.nhs.prm.deduction.e2e.queue.ThinlyWrappedSqsClient;
@Component
public class NemsEventProcessorDeadLetterQueue extends NemsEventMessageQueue {

    @Autowired
    public NemsEventProcessorDeadLetterQueue(ThinlyWrappedSqsClient thinlyWrappedSqsClient, TestConfiguration configuration) {
        super(thinlyWrappedSqsClient, configuration.nemsEventProcessorDeadLetterQueue());
    }
}
