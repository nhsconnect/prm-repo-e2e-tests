package uk.nhs.prm.e2etests.queue.end_of_transfer_service;

import org.springframework.beans.factory.annotation.Autowired;
import uk.nhs.prm.e2etests.property.QueueProperties;
import uk.nhs.prm.e2etests.client.ThinlyWrappedSqsClient;
import uk.nhs.prm.e2etests.queue.QueueMessageHelper;
import org.springframework.stereotype.Component;

@Component
public class EndOfTransferMofUpdatedMessageQueue extends QueueMessageHelper {
    @Autowired
    public EndOfTransferMofUpdatedMessageQueue(
            ThinlyWrappedSqsClient thinlyWrappedSqsClient,
            QueueProperties queueProperties) {
        super(thinlyWrappedSqsClient,
                queueProperties.getEndOfTransferServiceMofUpdatedQueueUrl());
    }
}
