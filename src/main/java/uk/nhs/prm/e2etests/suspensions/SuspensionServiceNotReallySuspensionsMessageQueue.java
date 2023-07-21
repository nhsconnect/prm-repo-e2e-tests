package uk.nhs.prm.e2etests.suspensions;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.nhs.prm.e2etests.configuration.QueuePropertySource;
import uk.nhs.prm.e2etests.queue.QueueMessageHelper;
import uk.nhs.prm.e2etests.queue.ThinlyWrappedSqsClient;

// TODO: PRMT-3488 - "NotReally" is a bizzare name for a queue, rename as fit.

@Component
public class SuspensionServiceNotReallySuspensionsMessageQueue extends QueueMessageHelper {

    @Autowired
    public SuspensionServiceNotReallySuspensionsMessageQueue(
            ThinlyWrappedSqsClient thinlyWrappedSqsClient,
            QueuePropertySource queuePropertySource
    ) {
        super(thinlyWrappedSqsClient,
              queuePropertySource.getSuspensionsServiceNotSuspendedObservabilityQueueUrl());
    }
}
