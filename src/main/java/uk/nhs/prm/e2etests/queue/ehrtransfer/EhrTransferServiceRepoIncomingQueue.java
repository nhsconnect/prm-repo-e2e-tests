package uk.nhs.prm.e2etests.queue.ehrtransfer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.nhs.prm.e2etests.model.RepoIncomingMessage;
import uk.nhs.prm.e2etests.property.QueueProperties;
import uk.nhs.prm.e2etests.queue.AbstractMessageQueue;
import uk.nhs.prm.e2etests.service.SqsService;

import java.util.Map;

@Component
public class EhrTransferServiceRepoIncomingQueue extends AbstractMessageQueue {
    @Autowired
    public EhrTransferServiceRepoIncomingQueue(
            SqsService sqsService,
            QueueProperties queueProperties
    ) {
        super(sqsService, queueProperties.getEhrTransferServiceRepoIncomingQueueUrl());
    }

    public void send(RepoIncomingMessage message) {
        super.postAMessageWithAttributes(message.toJsonString(),
                Map.of("conversationId", message.getInboundConversationId(),
                        "traceId", message.getInboundConversationId()));
    }
}
