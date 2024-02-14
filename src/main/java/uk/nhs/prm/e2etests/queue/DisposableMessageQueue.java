package uk.nhs.prm.e2etests.queue;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.nhs.prm.e2etests.service.SnsService;
import uk.nhs.prm.e2etests.service.SqsService;

@Component
public abstract class DisposableMessageQueue extends AbstractMessageQueue {
    protected final SqsService sqsService;
    protected final SnsService snsService;
    protected final String queueName;
    protected final String queueArn;
    protected final String queueUrl;
    protected final String topicArn;
    @Autowired
    public DisposableMessageQueue(
            SqsService sqsService,
            SnsService snsService,
            String queueName,
            String queueArn,
            String queueUrl,
            String topicArn) {
        super(sqsService, queueUrl);
        this.sqsService = sqsService;
        this.snsService = snsService;
        this.queueName = queueName;
        this.queueArn = queueArn;
        this.queueUrl = queueUrl;
        this.topicArn = topicArn;
    }

    @PostConstruct
    public void init() {
        this.sqsService.createQueue(this.queueName);
        this.snsService.subscribeQueueToTopic(
                this.queueArn,
                this.topicArn);
    }

    @PreDestroy
    public void cleanUp() {
        this.sqsService.deleteQueue(this.queueUrl);
    }
}