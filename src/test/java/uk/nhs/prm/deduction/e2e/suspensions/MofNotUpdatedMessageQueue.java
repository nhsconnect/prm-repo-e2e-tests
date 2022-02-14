package uk.nhs.prm.deduction.e2e.suspensions;

import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.model.Message;
import uk.nhs.prm.deduction.e2e.TestConfiguration;
import uk.nhs.prm.deduction.e2e.queue.SqsQueue;

import java.util.List;

@Component
public class MofNotUpdatedMessageQueue extends SuspensionMessageQueue {
    @Autowired
    public MofNotUpdatedMessageQueue(SqsQueue sqsQueue, TestConfiguration configuration) {
        super(sqsQueue, configuration.mofNotUpdatedQueueUri());
    }
}
