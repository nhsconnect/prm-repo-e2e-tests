package uk.nhs.prm.e2etests.queue.nems.observability;

import org.springframework.beans.factory.annotation.Autowired;
import uk.nhs.prm.e2etests.property.TopicProperties;
import uk.nhs.prm.e2etests.property.QueueProperties;
import org.springframework.stereotype.Component;
import uk.nhs.prm.e2etests.queue.DisposableNemsEventMessageQueue;
import uk.nhs.prm.e2etests.service.SnsService;
import uk.nhs.prm.e2etests.service.SqsService;

@Component
public class NemsEventProcessorReRegistrationsOQ extends DisposableNemsEventMessageQueue {
    @Autowired
    public NemsEventProcessorReRegistrationsOQ(
            SqsService sqsService,
            SnsService snsService,
            QueueProperties queueProperties,
            TopicProperties topicProperties) {
        super(
                sqsService,
                snsService,
                queueProperties.getNemsEventProcessorReregistrationObservabilityQueueName(),
                queueProperties.getNemsEventProcessorReregistrationObservabilityQueueArn(),
                queueProperties.getNemsEventProcessorReregistrationObservabilityQueueUrl(),
                topicProperties.getNemsEventProcessorRegistrationTopicArn()
        );
    }
}