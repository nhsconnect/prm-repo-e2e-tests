package uk.nhs.prm.e2etests.queue.reregistration;

import org.springframework.beans.factory.annotation.Autowired;
import uk.nhs.prm.e2etests.property.QueueProperties;
import uk.nhs.prm.e2etests.client.ThinlyWrappedSqsClient;
import uk.nhs.prm.e2etests.queue.nems.NemsEventMessageQueue;
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