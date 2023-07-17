package uk.nhs.prm.e2etests.nems;

import org.springframework.beans.factory.annotation.Autowired;
import uk.nhs.prm.e2etests.queue.ThinlyWrappedSqsClient;
import org.springframework.stereotype.Component;
import uk.nhs.prm.deduction.e2e.TestConfiguration;
import uk.nhs.prm.deduction.e2e.queue.ThinlyWrappedSqsClient;

@Component
public class MeshForwarderQueue extends NemsEventMessageQueue {

    @Autowired
    public MeshForwarderQueue(ThinlyWrappedSqsClient thinlyWrappedSqsClient, TestConfiguration testConfiguration) {
        super(thinlyWrappedSqsClient, testConfiguration.meshForwarderObservabilityQueueUri());
    }
}
