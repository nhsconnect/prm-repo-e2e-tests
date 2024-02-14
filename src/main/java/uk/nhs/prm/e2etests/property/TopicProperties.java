package uk.nhs.prm.e2etests.property;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.nhs.prm.e2etests.configuration.ActiveRoleArn;


@Component
public class TopicProperties {
    private static final String TEMPLATE_TOPIC_ARN = "arn:aws:sns:eu-west-2:%s:%s";

    @Value("${aws.configuration.topicNames.meshForwarder.nemsEvents}")
    private String meshForwarderNemsEventsTopicName;

    @Value("${aws.configuration.topicNames.nemsEventProcessor.reregistration}")
    private String nemsEventProcessorRegistrationTopicName;

    private final String awsAccountNumber;

    @Autowired
    public TopicProperties(
            ActiveRoleArn activeRoleArn
    ) {
        this.awsAccountNumber = activeRoleArn.getAccountNo();
    }

    public String getMeshForwarderNemsEventsTopicArn() {
        return formatTopicArn(meshForwarderNemsEventsTopicName);
    }
    public String getNemsEventProcessorRegistrationTopicArn() { return formatTopicArn(nemsEventProcessorRegistrationTopicName); }

    private String formatTopicArn(String topicName) {
        return String.format(TEMPLATE_TOPIC_ARN, this.awsAccountNumber, topicName);
    }
}
