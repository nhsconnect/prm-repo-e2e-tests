package uk.nhs.prm.e2etests.nems;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.nhs.prm.e2etests.configuration.QueuePropertySource;
import uk.nhs.prm.e2etests.queue.ThinlyWrappedSqsClient;

@Component
public class NemsEventProcessorUnhandledQueue extends NemsEventMessageQueue {

    @Autowired
    public NemsEventProcessorUnhandledQueue(
            ThinlyWrappedSqsClient thinlyWrappedSqsClient,
            QueuePropertySource queuePropertySource
    ) {
        super(thinlyWrappedSqsClient,
              queuePropertySource.getNemsEventProcessorUnhandledEventsQueueUrl());
    }

}
