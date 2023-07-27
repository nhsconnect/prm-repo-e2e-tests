package uk.nhs.prm.e2etests.queue.nems;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.nhs.prm.e2etests.property.QueueProperties;
import uk.nhs.prm.e2etests.client.ThinlyWrappedSqsClient;

@Component
public class NemsEventProcessorUnhandledQueue extends NemsEventMessageQueue {

    @Autowired
    public NemsEventProcessorUnhandledQueue(
            ThinlyWrappedSqsClient thinlyWrappedSqsClient,
            QueueProperties queueProperties
    ) {
        super(thinlyWrappedSqsClient,
              queueProperties.getNemsEventProcessorUnhandledEventsQueueUrl());
    }

}
