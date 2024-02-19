package uk.nhs.prm.e2etests.queue.nems.observability;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.nhs.prm.e2etests.property.QueueProperties;
import uk.nhs.prm.e2etests.property.TopicProperties;
import uk.nhs.prm.e2etests.queue.DisposableNemsEventMessageQueue;
import uk.nhs.prm.e2etests.service.SnsService;
import uk.nhs.prm.e2etests.service.SqsService;

@Component
public class MeshForwarderOQ extends DisposableNemsEventMessageQueue {
    @Autowired
    public MeshForwarderOQ(
            SqsService sqsService,
            SnsService snsService,
            QueueProperties queueProperties,
            TopicProperties topicProperties) {
        super(
                sqsService,
                snsService,
                queueProperties.getMeshForwarderNemsEventsObservabilityQueueName(),
                queueProperties.getMeshForwarderNemsEventsObservabilityQueueArn(),
                queueProperties.getMeshForwarderNemsEventsObservabilityQueueUri(),
                topicProperties.getMeshForwarderNemsEventsTopicArn()
        );
    }
}