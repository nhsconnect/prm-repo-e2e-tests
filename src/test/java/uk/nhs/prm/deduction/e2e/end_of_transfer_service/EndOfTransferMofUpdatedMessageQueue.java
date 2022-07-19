package uk.nhs.prm.deduction.e2e.end_of_transfer_service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.nhs.prm.deduction.e2e.TestConfiguration;
import uk.nhs.prm.deduction.e2e.queue.QueueMessageHelper;
import uk.nhs.prm.deduction.e2e.queue.SqsQueue;

@Component
public class EndOfTransferMofUpdatedMessageQueue extends QueueMessageHelper {
    @Autowired
    public EndOfTransferMofUpdatedMessageQueue(SqsQueue sqsQueue, TestConfiguration configuration) {
        super(sqsQueue, configuration.endOfTransferMofUpdatedQueue());
    }
}
