package uk.nhs.prm.e2etests.suspensions;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.nhs.prm.e2etests.property.QueueProperties;
import uk.nhs.prm.e2etests.queue.QueueMessageHelper;
import uk.nhs.prm.e2etests.queue.ThinlyWrappedSqsClient;

@Component
public class SuspensionMessageObservabilityQueue extends QueueMessageHelper {

    @Autowired
    public SuspensionMessageObservabilityQueue(
            ThinlyWrappedSqsClient thinlyWrappedSqsClient,
            QueueProperties queueProperties
    ) {
        super(thinlyWrappedSqsClient,
              queueProperties.getNemsEventProcessorSuspensionsObservabilityQueueUrl());
    }
}
