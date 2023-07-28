package uk.nhs.prm.e2etests.queue.suspensions;

import org.springframework.beans.factory.annotation.Autowired;
import uk.nhs.prm.e2etests.property.QueueProperties;
import uk.nhs.prm.e2etests.queue.AbstractMessageQueue;
import org.springframework.stereotype.Component;
import uk.nhs.prm.e2etests.service.SqsService;

@Component
public class SuspensionServiceDeceasedPatientQueue extends AbstractMessageQueue {
    @Autowired
    public SuspensionServiceDeceasedPatientQueue(
            SqsService sqsService,
            QueueProperties queueProperties
    ) {
        super(sqsService, queueProperties.getSuspensionServiceDeceasedPatientQueueUrl());
    }
}