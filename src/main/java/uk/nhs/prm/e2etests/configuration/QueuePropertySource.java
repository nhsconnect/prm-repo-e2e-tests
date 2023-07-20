package uk.nhs.prm.e2etests.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.nhs.prm.e2etests.services.SsmService;
import lombok.AccessLevel;
import lombok.Getter;

@Getter
@Component
public class QueuePropertySource extends AbstractSsmRetriever {
    @Value("${aws.configuration.queueUri.meshForwarder.nemsEventsObservability}")
    private String nemsEventsObservabilityQueueUri;

    @Value("${aws.configuration.queueUri.nemsEventProcessor.unhandledEvents}")
    private String nemsEventProcessorUnhandledEventsQueueUri;

    @Value("${aws.configuration.queueUri.nemsEventProcessor.suspensionsObservability}")
    private String nemsEventProcessorSuspensionsObservabilityQueueUri;

    @Value("${aws.configuration.queueUri.nemsEventProcessor.reregistrationObservability}")
    private String nemsEventProcessorReregistrationObservabilityQueueUri;

    @Value("${aws.configuration.queueUri.nemsEventProcessor.dlq}")
    private String nemsEventProcessorDlqQueueUri;

    @Value("${aws.configuration.queueUri.ehrTransferService.repoIncoming}")
    private String ehrTransferServiceRepoIncomingQueueUri;

    @Value("${aws.configuration.queueUri.suspensionsService.suspensions}")
    private String suspensionsServiceSuspensionsQueueUri;

    @Value("${aws.configuration.queueUri.suspensionsService.repoIncomingObservability}")
    private String suspensionsServiceRepoIncomingObservabilityQueueUri;

    @Value("${aws.configuration.queueUri.suspensionsService.notSuspendedObservability}")
    private String suspensionsServiceNotSuspendedObservabilityQueueUri;

    @Value("${aws.configuration.queueUri.suspensionsService.mofUpdated}")
    private String suspensionsServiceMofUpdatedQueueUri;

    @Value("${aws.configuration.queueUri.suspensionsService.mofNotUpdated}")
    private String suspensionsServiceMofNotUpdatedQueueUri;

    @Value("${aws.configuration.queueUri.suspensionsService.deceasedPatient}")
    private String suspensionServiceDeceasedPatientQueueUri;

    @Value("${aws.configuration.queueUri.ehrTransferService.smallEhrObservability}")
    private String ehrTransferServiceSmallEhrObservabilityQueueUri;

    @Value("${aws.configuration.queueUri.ehrTransferService.largeEhrObservability}")
    private String ehrTransferServiceLargeEhrObservabilityQueueUri;

    @Value("${aws.configuration.queueUri.ehrTransferService.largeMessageFragmentsObservability}")
    private String ehrTransferServiceLargeMessageFragmentsObservabilityQueueUri;

    @Value("${aws.configuration.queueUri.ehrTransferService.unhandledObservability}")
    private String ehrTransferServiceUnhandledObservabilityQueueUri;

    @Value("${aws.configuration.queueUri.ehrTransferService.negativeAcknowledgementsObservability}")
    private String ehrTransferServiceNegativeAcknowledgementObservabilityQueueUri;

    @Value("${aws.configuration.queueUri.ehrTransferService.ehrCompleteObservability}")
    private String ehrTransferServiceEhrCompleteObservabilityQueueUri;

    @Value("${aws.configuration.queueUri.ehrTransferService.parsingDlq}")
    private String ehrTransferServiceParsingDlqQueueUri;

    @Value("${aws.configuration.queueUri.endOfTransferService.mofUpdated}")
    private String endOfTransferServiceMofUpdatedQueueUri;

    @Value("${aws.configuration.queueUri.endOfTransferService.transferCompleteObservability}")
    private String endOfTransferServiceTransferCompleteObservabilityQueueUri;

    @Value("${aws.configuration.queueUri.gp2gpMessenger.messageSentObservability}")
    private String gp2gpMessengerObservabilityQueueUri;

    @Getter(AccessLevel.NONE)
    @Value("${aws.configuration.ssm.parameters.queue.mqAppUsername}")
    private String mqAppUsername;

    @Getter(AccessLevel.NONE)
    @Value("${aws.configuration.ssm.parameters.queue.mqAppPassword}")
    private String mqAppPassword;

    @Autowired
    public QueuePropertySource(SsmService ssmService) {
        super(ssmService);
    }

    public String getMqAppUsername() {
        return super.getAwsSsmParameterValue(this.mqAppUsername);
    }

    public String getMqAppPassword() {
        return super.getAwsSsmParameterValue(this.mqAppPassword);
    }
}