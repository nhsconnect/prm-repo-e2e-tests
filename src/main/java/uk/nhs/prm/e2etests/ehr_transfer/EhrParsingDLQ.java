package uk.nhs.prm.e2etests.ehr_transfer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.nhs.prm.e2etests.property.QueueProperties;
import uk.nhs.prm.e2etests.queue.QueueMessageHelper;
import uk.nhs.prm.e2etests.queue.ThinlyWrappedSqsClient;

@Component
public class EhrParsingDLQ extends QueueMessageHelper {

    @Autowired
    public EhrParsingDLQ(
            ThinlyWrappedSqsClient thinlyWrappedSqsClient,
            QueueProperties queueProperties
    ) {
        super(thinlyWrappedSqsClient,
              queueProperties.getEhrTransferServiceParsingDlqQueueUrl());
    }
}
