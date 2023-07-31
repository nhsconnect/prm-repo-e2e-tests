package uk.nhs.prm.e2etests.queue.ehrtransfer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.nhs.prm.e2etests.property.QueueProperties;
import uk.nhs.prm.e2etests.queue.AbstractMessageQueue;
import uk.nhs.prm.e2etests.service.SqsService;

@Component
public class EhrTransferServiceParsingDeadLetterQueue extends AbstractMessageQueue {
    @Autowired
    public EhrTransferServiceParsingDeadLetterQueue(
            SqsService sqsService,
            QueueProperties queueProperties
    ) {
        super(sqsService, queueProperties.getEhrTransferServiceParsingDlqQueueUrl());
    }
}