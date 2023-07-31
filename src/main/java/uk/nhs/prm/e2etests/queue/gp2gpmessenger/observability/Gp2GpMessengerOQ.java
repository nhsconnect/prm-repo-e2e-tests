package uk.nhs.prm.e2etests.queue.gp2gpmessenger.observability;

import org.springframework.beans.factory.annotation.Autowired;
import uk.nhs.prm.e2etests.property.QueueProperties;
import uk.nhs.prm.e2etests.queue.AbstractMessageQueue;
import org.springframework.stereotype.Component;
import uk.nhs.prm.e2etests.service.SqsService;

@Component
public class Gp2GpMessengerOQ extends AbstractMessageQueue {
    @Autowired
    public Gp2GpMessengerOQ(
            SqsService sqsService,
            QueueProperties queueProperties
    ) {
        super(sqsService, queueProperties.getGp2gpMessengerObservabilityQueueUrl());
    }
}
