package uk.nhs.prm.deduction.e2e.suspensions;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.nhs.prm.deduction.e2e.TestConfiguration;
import uk.nhs.prm.deduction.e2e.queue.SqsQueue;

@Component
public class SuspensionServiceNotReallySuspensionsMessageQueue extends SuspensionMessageQueue {

    @Autowired
    public SuspensionServiceNotReallySuspensionsMessageQueue(SqsQueue sqsQueue, TestConfiguration configuration) {
        super(sqsQueue, configuration.notReallySuspendedObservabilityQueueUri());
    }
}
