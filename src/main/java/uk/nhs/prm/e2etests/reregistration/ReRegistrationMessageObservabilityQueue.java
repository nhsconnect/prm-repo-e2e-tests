package uk.nhs.prm.e2etests.reregistration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.nhs.prm.e2etests.queue.ThinlyWrappedSqsClient;
import uk.nhs.prm.e2etests.TestConfiguration;
import uk.nhs.prm.e2etests.nems.NemsEventMessageQueue;

@Component
public class ReRegistrationMessageObservabilityQueue extends NemsEventMessageQueue {

    @Autowired
    public ReRegistrationMessageObservabilityQueue(ThinlyWrappedSqsClient thinlyWrappedSqsClient, TestConfiguration configuration) {
        super(thinlyWrappedSqsClient, configuration.reRegistrationObservabilityQueueUri());
    }

}
