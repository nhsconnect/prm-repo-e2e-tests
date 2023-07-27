package uk.nhs.prm.e2etests.queue.ehr_transfer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.nhs.prm.e2etests.property.QueueProperties;
import uk.nhs.prm.e2etests.queue.QueueMessageHelper;
import uk.nhs.prm.e2etests.client.ThinlyWrappedSqsClient;

@Component
public class LargeEhrFragmentsQueue extends QueueMessageHelper {
    @Autowired
    public LargeEhrFragmentsQueue(
            ThinlyWrappedSqsClient thinlyWrappedSqsClient,
            QueueProperties queueProperties
    ) {
        super(thinlyWrappedSqsClient,
              queueProperties.getEhrTransferServiceLargeMessageFragmentsObservabilityQueueUrl());
    }
}
