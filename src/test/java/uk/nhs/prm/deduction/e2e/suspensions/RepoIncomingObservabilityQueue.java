package uk.nhs.prm.deduction.e2e.suspensions;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.nhs.prm.e2etests.TestConfiguration;
import uk.nhs.prm.deduction.e2e.queue.QueueMessageHelper;
import uk.nhs.prm.deduction.e2e.queue.ThinlyWrappedSqsClient;

@Component
public class RepoIncomingObservabilityQueue extends QueueMessageHelper {

    @Autowired
    public RepoIncomingObservabilityQueue(ThinlyWrappedSqsClient thinlyWrappedSqsClient, TestConfiguration configuration) {
        super(thinlyWrappedSqsClient, configuration.repoIncomingObservabilityQueueUri());
    }
}
