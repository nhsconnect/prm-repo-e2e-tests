package uk.nhs.prm.e2etests.property;

import lombok.Getter;
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

    @Value("${aws.configuration.stringTemplates.queue.url}")
    private String TEMPLATE_QUEUE_URL;

    @Value("${aws.configuration.stringTemplates.queue.arn}")
    private String TEMPLATE_QUEUE_ARN;

    @Getter
    @Value("${aws.configuration.queueNames.meshForwarder.nemsEventsObservability}")
    private String meshForwarderNemsEventsObservabilityQueueName;

    @Value("${aws.configuration.queueNames.nemsEventProcessor.unhandledEvents}")
    private String nemsEventProcessorUnhandledEventsQueueName;

    @Value("${aws.configuration.queueNames.nemsEventProcessor.suspensionsObservability}")
    private String nemsEventProcessorSuspensionsObservabilityQueueName;

    @Getter
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

    @Value("${aws.configuration.ssm.parameters.queue.amqpEndpoint0}")
    private String amqpEndpoint0;

    @Value("${aws.configuration.ssm.parameters.queue.amqpEndpoint1}")
    private String amqpEndpoint1;

    @Value("${aws.configuration.ssm.parameters.queue.mqAppUsername}")
    private String mqAppUsername;

    @Value("${aws.configuration.ssm.parameters.queue.mqAppPassword}")
    private String mqAppPassword;

    private final String awsAccountNumber;

    private final SsmService ssmService;

    @Autowired
    public QueueProperties(
            SsmService ssmService,
            NhsProperties nhsProperties,
            ActiveRoleArn activeRoleArn
    ) {
        this.ssmService = ssmService;
        this.awsAccountNumber = activeRoleArn.getAccountNo();
    }

    public String getMqAppUsername() {
        return this.ssmService.getSsmParameterValue(this.mqAppUsername);
    }

    public String getMqAppPassword() {
        return this.ssmService.getSsmParameterValue(this.mqAppPassword);
    }

    public String getMeshForwarderNemsEventsObservabilityQueueUri() {
        return formatQueueUrl(meshForwarderNemsEventsObservabilityQueueName);
    }

    public String getMeshForwarderNemsEventsObservabilityQueueArn() {
        return formatQueueArn(meshForwarderNemsEventsObservabilityQueueName);
    }

    public String getNemsEventProcessorUnhandledEventsQueueUrl() {
        return formatQueueUrl(nemsEventProcessorUnhandledEventsQueueName);
    }

    public String getNemsEventProcessorSuspensionsObservabilityQueueUrl() {
        return formatQueueUrl(nemsEventProcessorSuspensionsObservabilityQueueName);
    }

    public String getNemsEventProcessorReregistrationObservabilityQueueUrl() {
        return formatQueueUrl(nemsEventProcessorReregistrationObservabilityQueueName);
    }

    public String getNemsEventProcessorReregistrationObservabilityQueueArn() {
        return formatQueueArn(nemsEventProcessorReregistrationObservabilityQueueName);
    }

    public String getNemsEventProcessorDlqQueueUrl() {
        return formatQueueUrl(nemsEventProcessorDlqQueueName);
    }

    public String getEhrTransferServiceRepoIncomingQueueUrl() {
        return formatQueueUrl(ehrTransferServiceRepoIncomingQueueName);
    }

    public String getSuspensionsServiceSuspensionsQueueUrl() {
        return formatQueueUrl(suspensionsServiceSuspensionsQueueName);
    }

    public String getSuspensionsServiceRepoIncomingObservabilityQueueUrl() {
        return formatQueueUrl(suspensionsServiceRepoIncomingObservabilityQueueName);
    }

    public String getSuspensionsServiceNotSuspendedObservabilityQueueUrl() {
        return formatQueueUrl(suspensionsServiceNotSuspendedObservabilityQueueName);
    }

    public String getSuspensionsServiceMofUpdatedQueueUrl() {
        return formatQueueUrl(suspensionsServiceMofUpdatedQueueName);
    }

    public String getSuspensionsServiceMofNotUpdatedQueueUrl() {
        return formatQueueUrl(suspensionsServiceMofNotUpdatedQueueName);
    }

    public String getSuspensionServiceDeceasedPatientQueueUrl() {
        return formatQueueUrl(suspensionServiceDeceasedPatientQueueName);
    }

    public String getEhrTransferServiceSmallEhrObservabilityQueueUrl() {
        return formatQueueUrl(ehrTransferServiceSmallEhrObservabilityQueueName);
    }

    public String getEhrTransferServiceLargeEhrObservabilityQueueUrl() {
        return formatQueueUrl(ehrTransferServiceLargeEhrObservabilityQueueName);
    }

    public String getEhrTransferServiceLargeMessageFragmentsObservabilityQueueUrl() {
        return formatQueueUrl(ehrTransferServiceLargeMessageFragmentsObservabilityQueueName);
    }

    public String getEhrTransferServiceUnhandledObservabilityQueueUrl() {
        return formatQueueUrl(ehrTransferServiceUnhandledObservabilityQueueName);
    }

    public String getEhrTransferServiceNegativeAcknowledgementObservabilityQueueUrl() {
        return formatQueueUrl(ehrTransferServiceNegativeAcknowledgementObservabilityQueueName);
    }

    public String getEhrTransferServiceEhrCompleteObservabilityQueueUrl() {
        return formatQueueUrl(ehrTransferServiceEhrCompleteObservabilityQueueName);
    }

    public String getEhrTransferServiceParsingDlqQueueUrl() {
        return formatQueueUrl(ehrTransferServiceParsingDlqQueueName);
    }

    public String getEndOfTransferServiceMofUpdatedQueueUrl() {
        return formatQueueUrl(endOfTransferServiceMofUpdatedQueueName);
    }

    public String getEndOfTransferServiceTransferCompleteObservabilityQueueUrl() {
        return formatQueueUrl(endOfTransferServiceTransferCompleteObservabilityQueueName);
    }

    public String getGp2gpMessengerObservabilityQueueUrl() {
        return formatQueueUrl(gp2gpMessengerObservabilityQueueName);
    }

    public AmqpEndpoint getAmqpEndpoint0() {
        return formatAmqpEndpoint(ssmService.getSsmParameterValue(amqpEndpoint0));
    }

    public AmqpEndpoint getAmqpEndpoint1() {
        return formatAmqpEndpoint(ssmService.getSsmParameterValue(amqpEndpoint1));
    }

    private String formatQueueUrl(String queueName) {
        return String.format(TEMPLATE_QUEUE_URL, awsAccountNumber, queueName);
    }

    private String formatQueueArn(String queueName) {
        return String.format(TEMPLATE_QUEUE_ARN, awsAccountNumber, queueName);
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
