package uk.nhs.prm.e2etests.ehr_transfer;

import org.springframework.beans.factory.annotation.Autowired;
import uk.nhs.prm.e2etests.configuration.QueuePropertySource;
import uk.nhs.prm.e2etests.queue.ThinlyWrappedSqsClient;
import uk.nhs.prm.e2etests.queue.QueueMessageHelper;
import org.springframework.stereotype.Component;

@Component
public class Gp2gpMessengerQueue extends QueueMessageHelper {
    @Autowired
    public Gp2gpMessengerQueue(
            ThinlyWrappedSqsClient thinlyWrappedSqsClient,
            QueuePropertySource queuePropertySource
    ) {
        super(thinlyWrappedSqsClient,
              queuePropertySource.getGp2gpMessengerObservabilityQueueUri());
    }
}
