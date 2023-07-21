package uk.nhs.prm.e2etests.suspensions;

import org.springframework.beans.factory.annotation.Autowired;
import uk.nhs.prm.e2etests.configuration.QueuePropertySource;
import uk.nhs.prm.e2etests.queue.ThinlyWrappedSqsClient;
import uk.nhs.prm.e2etests.queue.QueueMessageHelper;
import org.springframework.stereotype.Component;

// TODO PRMT-3488 'real' queue? What's a 'fake' queue? Does this need a rename?
@Component
public class SuspensionMessageRealQueue extends QueueMessageHelper {

    @Autowired
    public SuspensionMessageRealQueue(
            ThinlyWrappedSqsClient thinlyWrappedSqsClient,
            QueuePropertySource queuePropertySource
    ) {
        super(thinlyWrappedSqsClient, queuePropertySource.getSuspensionsServiceSuspensionsQueueUrl());
    }
}
