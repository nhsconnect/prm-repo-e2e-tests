package uk.nhs.prm.deduction.e2e.ehr_transfer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.nhs.prm.deduction.e2e.TestConfiguration;
import uk.nhs.prm.deduction.e2e.models.RepoIncomingMessage;
import uk.nhs.prm.deduction.e2e.queue.QueueMessageHelper;
import uk.nhs.prm.deduction.e2e.queue.ThinlyWrappedSqsClient;

@Component
public class RepoIncomingQueue extends QueueMessageHelper {

    @Autowired
    public RepoIncomingQueue(ThinlyWrappedSqsClient thinlyWrappedSqsClient, TestConfiguration configuration) {
        super(thinlyWrappedSqsClient, configuration.repoIncomingQueueUri());
    }

    public void send(RepoIncomingMessage message) {
        super.postAMessageWithAttribute(message.toJsonString(), "conversationId", message.conversationId());
    }
}
