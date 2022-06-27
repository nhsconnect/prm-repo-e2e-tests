package uk.nhs.prm.deduction.e2e.ehr_transfer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.nhs.prm.deduction.e2e.TestConfiguration;
import uk.nhs.prm.deduction.e2e.queue.QueueMessageHelper;
import uk.nhs.prm.deduction.e2e.queue.SqsQueue;

@Component
public class NegativeAcknowledgementQueue extends QueueMessageHelper {

    @Autowired
    public NegativeAcknowledgementQueue(SqsQueue sqsQueue, TestConfiguration configuration) {
        super(sqsQueue, configuration.negativeAcknowledgementQueueUri());
    }
}
