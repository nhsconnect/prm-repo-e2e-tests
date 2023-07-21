package uk.nhs.prm.e2etests.ehr_transfer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.nhs.prm.e2etests.configuration.QueuePropertySource;
import uk.nhs.prm.e2etests.model.RepoIncomingMessage;
import uk.nhs.prm.e2etests.queue.QueueMessageHelper;
import uk.nhs.prm.e2etests.queue.ThinlyWrappedSqsClient;

@Component
public class RepoIncomingQueue extends QueueMessageHelper {

    @Autowired
    public RepoIncomingQueue(
            ThinlyWrappedSqsClient thinlyWrappedSqsClient,
            QueuePropertySource queuePropertySource
    ) {
        super(thinlyWrappedSqsClient,
              queuePropertySource.getEhrTransferServiceRepoIncomingQueueUrl());
    }

    public void send(RepoIncomingMessage message) {
        super.postAMessageWithAttribute(message.toJsonString(), "conversationId", message.conversationId());
    }
}
