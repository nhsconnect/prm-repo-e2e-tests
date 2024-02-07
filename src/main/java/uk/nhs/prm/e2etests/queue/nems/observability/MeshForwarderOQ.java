package uk.nhs.prm.e2etests.queue.nems.observability;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.nhs.prm.e2etests.property.QueueProperties;
import uk.nhs.prm.e2etests.property.TopicProperties;
import uk.nhs.prm.e2etests.queue.AbstractNemsEventMessageQueue;
import uk.nhs.prm.e2etests.service.SnsService;
import uk.nhs.prm.e2etests.service.SqsService;

@Component
public class MeshForwarderOQ extends AbstractNemsEventMessageQueue {
    protected final SqsService sqsService;
    protected final SnsService snsService;
    protected final QueueProperties queueProperties;
    protected final TopicProperties topicProperties;

    @Autowired
    public MeshForwarderOQ(
            SqsService sqsService,
            SnsService snsService,
            QueueProperties queueProperties,
            TopicProperties topicProperties) {
        super(sqsService, queueProperties.getMeshForwarderNemsEventsObservabilityQueueUri());
        this.sqsService = sqsService;
        this.snsService = snsService;
        this.queueProperties = queueProperties;
        this.topicProperties = topicProperties;
    }

    @PostConstruct
    public void init() {
        this.sqsService.createQueue(this.queueProperties.getMeshForwarderNemsEventsObservabilityQueueName());
        this.snsService.subscribeQueueToTopic(
                this.queueProperties.getMeshForwarderNemsEventsObservabilityQueueUri(),
                this.topicProperties.getMeshForwarderNemsEventsTopicArn());
    }

    @PreDestroy
    public void cleanUp() {
        this.sqsService.deleteQueue(this.queueProperties.getMeshForwarderNemsEventsObservabilityQueueUri());
    }
}