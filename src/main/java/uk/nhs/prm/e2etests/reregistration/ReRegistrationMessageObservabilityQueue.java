package uk.nhs.prm.e2etests.reregistration;

import org.springframework.beans.factory.annotation.Autowired;
import uk.nhs.prm.e2etests.property.QueueProperties;
import uk.nhs.prm.e2etests.queue.ThinlyWrappedSqsClient;
import uk.nhs.prm.e2etests.nems.NemsEventMessageQueue;
import org.springframework.stereotype.Component;

@Component
public class ReRegistrationMessageObservabilityQueue extends NemsEventMessageQueue {
    @Autowired
    public ReRegistrationMessageObservabilityQueue(
            ThinlyWrappedSqsClient thinlyWrappedSqsClient,
            QueueProperties queueProperties) {
        super(thinlyWrappedSqsClient, queueProperties.getNemsEventProcessorReregistrationObservabilityQueueUrl());
    }
}