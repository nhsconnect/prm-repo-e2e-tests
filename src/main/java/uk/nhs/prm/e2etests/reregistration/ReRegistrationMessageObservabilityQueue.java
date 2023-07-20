package uk.nhs.prm.e2etests.reregistration;

import org.springframework.beans.factory.annotation.Autowired;
import uk.nhs.prm.e2etests.configuration.QueuePropertySource;
import uk.nhs.prm.e2etests.queue.ThinlyWrappedSqsClient;
import uk.nhs.prm.e2etests.nems.NemsEventMessageQueue;
import org.springframework.stereotype.Component;

@Component
public class ReRegistrationMessageObservabilityQueue extends NemsEventMessageQueue {
    @Autowired
    public ReRegistrationMessageObservabilityQueue(
            ThinlyWrappedSqsClient thinlyWrappedSqsClient,
            QueuePropertySource queuePropertySource) {
        super(thinlyWrappedSqsClient, queuePropertySource.getNemsEventProcessorReregistrationObservabilityQueueUri());
    }
}