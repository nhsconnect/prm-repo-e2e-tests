package uk.nhs.prm.e2etests.queue.ehr_transfer;

import org.springframework.beans.factory.annotation.Autowired;
import uk.nhs.prm.e2etests.property.QueueProperties;
import uk.nhs.prm.e2etests.client.ThinlyWrappedSqsClient;
import uk.nhs.prm.e2etests.queue.QueueMessageHelper;
import org.springframework.stereotype.Component;

@Component
public class SmallEhrQueue extends QueueMessageHelper {
    @Autowired
    public SmallEhrQueue(
            ThinlyWrappedSqsClient thinlyWrappedSqsClient,
            QueueProperties queueProperties
    ) {
        super(thinlyWrappedSqsClient,
              queueProperties.getEhrTransferServiceSmallEhrObservabilityQueueUrl());
    }
}
