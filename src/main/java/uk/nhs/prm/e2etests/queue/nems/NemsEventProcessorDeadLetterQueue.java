package uk.nhs.prm.e2etests.queue.nems;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.nhs.prm.e2etests.property.QueueProperties;
import uk.nhs.prm.e2etests.client.ThinlyWrappedSqsClient;

@Component
public class NemsEventProcessorDeadLetterQueue extends NemsEventMessageQueue {

    @Autowired
    public NemsEventProcessorDeadLetterQueue(
            ThinlyWrappedSqsClient thinlyWrappedSqsClient,
            QueueProperties queueProperties
    ) {
        super(thinlyWrappedSqsClient,
              queueProperties.getNemsEventProcessorDlqQueueUrl());
    }
}
