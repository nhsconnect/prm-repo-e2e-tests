package uk.nhs.prm.e2etests.queue.suspensions;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.nhs.prm.e2etests.property.QueueProperties;
import uk.nhs.prm.e2etests.queue.QueueMessageHelper;
import uk.nhs.prm.e2etests.client.ThinlyWrappedSqsClient;

@Component
public class RepoIncomingObservabilityQueue extends QueueMessageHelper {

    @Autowired
    public RepoIncomingObservabilityQueue(
            ThinlyWrappedSqsClient thinlyWrappedSqsClient,
            QueueProperties queueProperties
    ) {
        super(thinlyWrappedSqsClient,
              queueProperties.getSuspensionsServiceRepoIncomingObservabilityQueueUrl());
    }
}
