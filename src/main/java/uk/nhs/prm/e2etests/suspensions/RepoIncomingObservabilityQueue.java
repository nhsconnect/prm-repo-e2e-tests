package uk.nhs.prm.e2etests.suspensions;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.nhs.prm.e2etests.configuration.QueuePropertySource;
import uk.nhs.prm.e2etests.queue.QueueMessageHelper;
import uk.nhs.prm.e2etests.queue.ThinlyWrappedSqsClient;
import uk.nhs.prm.e2etests.TestConfiguration;

@Component
public class RepoIncomingObservabilityQueue extends QueueMessageHelper {

    @Autowired
    public RepoIncomingObservabilityQueue(
            ThinlyWrappedSqsClient thinlyWrappedSqsClient,
            QueuePropertySource queuePropertySource
    ) {
        super(thinlyWrappedSqsClient,
              queuePropertySource.getSuspensionsServiceRepoIncomingObservabilityQueueUri());
    }
}
