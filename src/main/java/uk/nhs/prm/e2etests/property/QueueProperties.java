package uk.nhs.prm.e2etests.property;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.nhs.prm.e2etests.configuration.ActiveRoleArn;
import uk.nhs.prm.e2etests.exception.InvalidAmqpEndpointException;
import uk.nhs.prm.e2etests.model.AmqpEndpoint;
import uk.nhs.prm.e2etests.service.SsmService;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class QueueProperties {
    private static final String TEMPLATE_QUEUE_URL = "https://sqs.eu-west-2.amazonaws.com/%s/%s-%s";
    private static final String TEMPLATE_QUEUE_ARN = "arn:aws:sqs:eu-west-2:%s:%s-%s";
    private static final String TEMPLATE_QUEUE_NAME = "%s-%s";

    @Value("${aws.configuration.queueNames.meshForwarder.nemsEventsObservability}")
    private String meshForwarderNemsEventsObservabilityQueueName;

    @Value("${aws.configuration.queueNames.nemsEventProcessor.unhandledEvents}")
    private String nemsEventProcessorUnhandledEventsQueueName;

    @Value("${aws.configuration.queueNames.nemsEventProcessor.suspensionsObservability}")
    private String nemsEventProcessorSuspensionsObservabilityQueueName;

    @Value("${aws.configuration.queueNames.nemsEventProcessor.reregistrationObservability}")
    private String nemsEventProcessorReregistrationObservabilityQueueName;

    @Value("${aws.configuration.queueNames.nemsEventProcessor.dlq}")
    private String nemsEventProcessorDlqQueueName;

    @Value("${aws.configuration.queueNames.ehrTransferService.repoIncoming}")
    private String ehrTransferServiceRepoIncomingQueueName;

    @Value("${aws.configuration.queueNames.suspensionsService.suspensions}")
    private String suspensionsServiceSuspensionsQueueName;

    @Value("${aws.configuration.queueNames.suspensionsService.repoIncomingObservability}")
    private String suspensionsServiceRepoIncomingObservabilityQueueName;

    @Value("${aws.configuration.queueNames.suspensionsService.notSuspendedObservability}")
    private String suspensionsServiceNotSuspendedObservabilityQueueName;

    @Value("${aws.configuration.queueNames.suspensionsService.mofUpdated}")
    private String suspensionsServiceMofUpdatedQueueName;

    @Value("${aws.configuration.queueNames.suspensionsService.mofNotUpdated}")
    private String suspensionsServiceMofNotUpdatedQueueName;

    @Value("${aws.configuration.queueNames.suspensionsService.deceasedPatient}")
    private String suspensionServiceDeceasedPatientQueueName;

    @Value("${aws.configuration.queueNames.ehrTransferService.smallEhrObservability}")
    private String ehrTransferServiceSmallEhrObservabilityQueueName;

    @Value("${aws.configuration.queueNames.ehrTransferService.largeEhrObservability}")
    private String ehrTransferServiceLargeEhrObservabilityQueueName;

    @Value("${aws.configuration.queueNames.ehrTransferService.largeMessageFragmentsObservability}")
    private String ehrTransferServiceLargeMessageFragmentsObservabilityQueueName;

    @Value("${aws.configuration.queueNames.ehrTransferService.unhandledObservability}")
    private String ehrTransferServiceUnhandledObservabilityQueueName;

    @Value("${aws.configuration.queueNames.ehrTransferService.negativeAcknowledgementsObservability}")
    private String ehrTransferServiceNegativeAcknowledgementObservabilityQueueName;

    @Value("${aws.configuration.queueNames.ehrTransferService.ehrCompleteObservability}")
    private String ehrTransferServiceEhrCompleteObservabilityQueueName;

    @Value("${aws.configuration.queueNames.ehrTransferService.parsingDlq}")
    private String ehrTransferServiceParsingDlqQueueName;

    @Value("${aws.configuration.queueNames.endOfTransferService.mofUpdated}")
    private String endOfTransferServiceMofUpdatedQueueName;

    @Value("${aws.configuration.queueNames.endOfTransferService.transferCompleteObservability}")
    private String endOfTransferServiceTransferCompleteObservabilityQueueName;

    @Value("${aws.configuration.queueNames.gp2gpMessenger.messageSentObservability}")
    private String gp2gpMessengerObservabilityQueueName;

    @Value("${aws.configuration.ssm.parameters.queue.amqpEndpoint}")
    private String amqpEndpoint;

    @Value("${aws.configuration.ssm.parameters.queue.mqAppUsername}")
    private String mqAppUsername;

    @Value("${aws.configuration.ssm.parameters.queue.mqAppPassword}")
    private String mqAppPassword;

    private final String nhsEnvironment;

    private final String awsAccountNumber;

    private final SsmService ssmService;

    @Autowired
    public QueueProperties(
            SsmService ssmService,
            NhsProperties nhsProperties,
            ActiveRoleArn activeRoleArn
    ) {
        this.ssmService = ssmService;
        this.nhsEnvironment = nhsProperties.getNhsEnvironment();
        this.awsAccountNumber = activeRoleArn.getAccountNo();
    }

    public String getMqAppUsername() {
        return this.ssmService.getSsmParameterValue(this.mqAppUsername);
    }

    public String getMqAppPassword() {
        return this.ssmService.getSsmParameterValue(this.mqAppPassword);
    }

    public String getMeshForwarderNemsEventsObservabilityQueueUri() {
        return getQueueUrl(meshForwarderNemsEventsObservabilityQueueName);
    }

    public String getMeshForwarderNemsEventsObservabilityQueueName() {
        return getQueueName(meshForwarderNemsEventsObservabilityQueueName);
    }

    public String getMeshForwarderNemsEventsObservabilityQueueArn() {
        return getQueueArn(meshForwarderNemsEventsObservabilityQueueName);
    }

    public String getNemsEventProcessorUnhandledEventsQueueUrl() {
        return getQueueUrl(nemsEventProcessorUnhandledEventsQueueName);
    }

    public String getNemsEventProcessorSuspensionsObservabilityQueueUrl() {
        return getQueueUrl(nemsEventProcessorSuspensionsObservabilityQueueName);
    }

    public String getNemsEventProcessorReregistrationObservabilityQueueUrl() {
        return getQueueUrl(nemsEventProcessorReregistrationObservabilityQueueName);
    }

    public String getNemsEventProcessorDlqQueueUrl() {
        return getQueueUrl(nemsEventProcessorDlqQueueName);
    }

    public String getEhrTransferServiceRepoIncomingQueueUrl() {
        return getQueueUrl(ehrTransferServiceRepoIncomingQueueName);
    }

    public String getSuspensionsServiceSuspensionsQueueUrl() {
        return getQueueUrl(suspensionsServiceSuspensionsQueueName);
    }

    public String getSuspensionsServiceRepoIncomingObservabilityQueueUrl() {
        return getQueueUrl(suspensionsServiceRepoIncomingObservabilityQueueName);
    }

    public String getSuspensionsServiceNotSuspendedObservabilityQueueUrl() {
        return getQueueUrl(suspensionsServiceNotSuspendedObservabilityQueueName);
    }

    public String getSuspensionsServiceMofUpdatedQueueUrl() {
        return getQueueUrl(suspensionsServiceMofUpdatedQueueName);
    }

    public String getSuspensionsServiceMofNotUpdatedQueueUrl() {
        return getQueueUrl(suspensionsServiceMofNotUpdatedQueueName);
    }

    public String getSuspensionServiceDeceasedPatientQueueUrl() {
        return getQueueUrl(suspensionServiceDeceasedPatientQueueName);
    }

    public String getEhrTransferServiceSmallEhrObservabilityQueueUrl() {
        return getQueueUrl(ehrTransferServiceSmallEhrObservabilityQueueName);
    }

    public String getEhrTransferServiceLargeEhrObservabilityQueueUrl() {
        return getQueueUrl(ehrTransferServiceLargeEhrObservabilityQueueName);
    }

    public String getEhrTransferServiceLargeMessageFragmentsObservabilityQueueUrl() {
        return getQueueUrl(ehrTransferServiceLargeMessageFragmentsObservabilityQueueName);
    }

    public String getEhrTransferServiceUnhandledObservabilityQueueUrl() {
        return getQueueUrl(ehrTransferServiceUnhandledObservabilityQueueName);
    }

    public String getEhrTransferServiceNegativeAcknowledgementObservabilityQueueUrl() {
        return getQueueUrl(ehrTransferServiceNegativeAcknowledgementObservabilityQueueName);
    }

    public String getEhrTransferServiceEhrCompleteObservabilityQueueUrl() {
        return getQueueUrl(ehrTransferServiceEhrCompleteObservabilityQueueName);
    }

    public String getEhrTransferServiceParsingDlqQueueUrl() {
        return getQueueUrl(ehrTransferServiceParsingDlqQueueName);
    }

    public String getEndOfTransferServiceMofUpdatedQueueUrl() {
        return getQueueUrl(endOfTransferServiceMofUpdatedQueueName);
    }

    public String getEndOfTransferServiceTransferCompleteObservabilityQueueUrl() {
        return getQueueUrl(endOfTransferServiceTransferCompleteObservabilityQueueName);
    }

    public String getGp2gpMessengerObservabilityQueueUrl() {
        return getQueueUrl(gp2gpMessengerObservabilityQueueName);
    }

    public AmqpEndpoint getAmqpEndpoint() {
        // In the event this fails, there is also an 'amqp-endpoint-1' in SSM
        return formatAmqpEndpoint(this.ssmService.getSsmParameterValue(this.amqpEndpoint));
    }

    private String getQueueUrl(String queueName) {
        return String.format(TEMPLATE_QUEUE_URL, this.awsAccountNumber, this.nhsEnvironment, queueName);
    }

    private String getQueueArn(String queueName) {
        return String.format(TEMPLATE_QUEUE_ARN, this.awsAccountNumber, this.nhsEnvironment, queueName);
    }

    private String getQueueName(String queueName) {
        return String.format(TEMPLATE_QUEUE_NAME, this.nhsEnvironment, queueName);
    }

    private AmqpEndpoint formatAmqpEndpoint(String endpoint) {
        // Regex is splitting amqp endpoint into protocol://hostname:port
        Pattern endpointRegex = Pattern.compile("(.+)://(.+):(.+)");
        Matcher matcher = endpointRegex.matcher(endpoint);

        if (matcher.find()) {
            return AmqpEndpoint.builder()
                    .protocol(matcher.group(1))
                    .hostname(matcher.group(2))
                    .port(Integer.parseInt(matcher.group(3)))
                    .build();
        } else {
            throw new InvalidAmqpEndpointException(endpoint);
        }
    }
}
