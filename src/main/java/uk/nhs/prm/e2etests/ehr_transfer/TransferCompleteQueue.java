package uk.nhs.prm.e2etests.ehr_transfer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.nhs.prm.e2etests.property.QueueProperties;
import uk.nhs.prm.e2etests.queue.QueueMessageHelper;
import uk.nhs.prm.e2etests.queue.ThinlyWrappedSqsClient;

@Component
public class TransferCompleteQueue extends QueueMessageHelper {
    @Autowired
    public TransferCompleteQueue(
            ThinlyWrappedSqsClient thinlyWrappedSqsClient,
            QueueProperties queueProperties
    ) {
        /*
        TODO PRMT-3487 Refactor this into the end_of_transfer_service directory? The logic by the previous
         team originally had this as the Ehr transfer service transfer complete observability qeueue but they changed it
         to be the end of transfer service and didn't change directories.
         */
        super(thinlyWrappedSqsClient,
                queueProperties.getEndOfTransferServiceTransferCompleteObservabilityQueueUrl());
    }
}
