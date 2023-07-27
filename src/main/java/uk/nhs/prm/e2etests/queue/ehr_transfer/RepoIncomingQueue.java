package uk.nhs.prm.e2etests.queue.ehr_transfer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.nhs.prm.e2etests.property.QueueProperties;
import uk.nhs.prm.e2etests.model.RepoIncomingMessage;
import uk.nhs.prm.e2etests.queue.QueueMessageHelper;
import uk.nhs.prm.e2etests.client.ThinlyWrappedSqsClient;

@Component
public class RepoIncomingQueue extends QueueMessageHelper {

    @Autowired
    public RepoIncomingQueue(
            ThinlyWrappedSqsClient thinlyWrappedSqsClient,
            QueueProperties queueProperties
    ) {
        super(thinlyWrappedSqsClient,
              queueProperties.getEhrTransferServiceRepoIncomingQueueUrl());
    }

    public void send(RepoIncomingMessage message) {
        super.postAMessageWithAttribute(message.toJsonString(), "conversationId", message.conversationId());
    }
}
