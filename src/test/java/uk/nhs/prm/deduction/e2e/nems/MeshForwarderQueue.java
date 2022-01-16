package uk.nhs.prm.deduction.e2e.nems;

import org.springframework.beans.factory.annotation.Autowired;
import software.amazon.awssdk.services.sqs.model.Message;
import org.springframework.stereotype.Component;
import uk.nhs.prm.deduction.e2e.TestConfiguration;
import uk.nhs.prm.deduction.e2e.queue.SqsQueue;

import java.util.List;

@Component
public class MeshForwarderQueue extends NemsEventMessageQueue {

    List<Message> forwarderMessages;

    @Autowired
    public MeshForwarderQueue(SqsQueue sqsQueue, TestConfiguration configuration) {
        super(sqsQueue, configuration.meshForwarderObservabilityQueueUri());
    }
}
