package uk.nhs.prm.e2etests.nems;

import org.springframework.beans.factory.annotation.Autowired;
import uk.nhs.prm.e2etests.property.QueueProperties;
import uk.nhs.prm.e2etests.queue.ThinlyWrappedSqsClient;
import org.springframework.stereotype.Component;


@Component
public class MeshForwarderQueue extends NemsEventMessageQueue {
    @Autowired
    public MeshForwarderQueue(
            ThinlyWrappedSqsClient thinlyWrappedSqsClient,
            QueueProperties queueProperties
    ) {
        super(thinlyWrappedSqsClient,
              queueProperties.getMeshForwarderNemsEventsObservabilityQueueUri());
    }
}
