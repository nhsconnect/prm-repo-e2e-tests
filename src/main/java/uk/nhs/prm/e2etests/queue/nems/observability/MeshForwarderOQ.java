package uk.nhs.prm.e2etests.queue.nems.observability;

import uk.nhs.prm.e2etests.queue.AbstractNemsEventMessageQueue;
import org.springframework.beans.factory.annotation.Autowired;
import uk.nhs.prm.e2etests.property.QueueProperties;
import org.springframework.stereotype.Component;
import uk.nhs.prm.e2etests.service.SqsService;

@Component
public class MeshForwarderOQ extends AbstractNemsEventMessageQueue {
    @Autowired
    public MeshForwarderOQ(
            SqsService sqsService,
            QueueProperties queueProperties
    ) {
        super(sqsService, queueProperties.getMeshForwarderNemsEventsObservabilityQueueUri());
    }
}