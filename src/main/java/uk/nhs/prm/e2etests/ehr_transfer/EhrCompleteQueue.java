package uk.nhs.prm.e2etests.ehr_transfer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.nhs.prm.e2etests.configuration.QueuePropertySource;
import uk.nhs.prm.e2etests.queue.QueueMessageHelper;
import uk.nhs.prm.e2etests.queue.ThinlyWrappedSqsClient;

@Component
public class EhrCompleteQueue extends QueueMessageHelper {

    @Autowired
    public EhrCompleteQueue(ThinlyWrappedSqsClient thinlyWrappedSqsClient, QueuePropertySource queuePropertySource) {
        super(thinlyWrappedSqsClient, queuePropertySource.getEhrTransferServiceEhrCompleteObservabilityQueueUrl());
    }
}