package uk.nhs.prm.e2etests.end_of_transfer_service;

import org.springframework.beans.factory.annotation.Autowired;
import uk.nhs.prm.e2etests.configuration.QueuePropertySource;
import uk.nhs.prm.e2etests.queue.ThinlyWrappedSqsClient;
import uk.nhs.prm.e2etests.queue.QueueMessageHelper;
import org.springframework.stereotype.Component;

@Component
public class EndOfTransferMofUpdatedMessageQueue extends QueueMessageHelper {
    @Autowired
    public EndOfTransferMofUpdatedMessageQueue(
            ThinlyWrappedSqsClient thinlyWrappedSqsClient,
            QueuePropertySource queuePropertySource) {
        super(thinlyWrappedSqsClient,
                queuePropertySource.getEndOfTransferServiceMofUpdatedQueueUrl());
    }
}
