package uk.nhs.prm.e2etests.suspensions;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.nhs.prm.e2etests.configuration.QueuePropertySource;
import uk.nhs.prm.e2etests.queue.QueueMessageHelper;
import uk.nhs.prm.e2etests.queue.ThinlyWrappedSqsClient;

@Component
public class MofUpdatedMessageQueue extends QueueMessageHelper {
    @Autowired
    public MofUpdatedMessageQueue(
            ThinlyWrappedSqsClient thinlyWrappedSqsClient,
            QueuePropertySource queuePropertySource
    ) {
        super(thinlyWrappedSqsClient,
              queuePropertySource.getSuspensionsServiceMofUpdatedQueueUrl());
    }
}
