package uk.nhs.prm.e2etests.queue.ehrtransfer.observability;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.nhs.prm.e2etests.property.QueueProperties;
import uk.nhs.prm.e2etests.queue.AbstractMessageQueue;
import uk.nhs.prm.e2etests.service.SqsService;

@Component
public class EhrTransferServiceEhrCompleteOQ extends AbstractMessageQueue {
    @Autowired
    public EhrTransferServiceEhrCompleteOQ(SqsService sqsService,
                                           QueueProperties queueProperties) {
        super(sqsService, queueProperties.getEhrTransferServiceEhrCompleteObservabilityQueueUrl());
    }
}