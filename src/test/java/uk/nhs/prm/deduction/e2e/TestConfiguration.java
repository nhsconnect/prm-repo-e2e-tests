package uk.nhs.prm.deduction.e2e;


import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.utils.ImmutableMap;
import uk.nhs.prm.deduction.e2e.client.AwsConfigurationClient;

import java.util.List;

import static java.util.Arrays.asList;

@Component
public class TestConfiguration {

    private final ImmutableMap<String, List<String>> suspendedNhsNumbersByEnv = ImmutableMap.of(
            "dev", asList(
                    "9693797396",
                    "9693797426",
                    "9693797477"),
            "pre-prod", asList(
                    "9693642422",
                    "9693642430",
                    "9693642449",
                    "9693642457",
                    "9693642465",
                    "9693642473",
                    "9693642481",
                    "9693642503",
                    "9693642511",
                    "9693642538")
    );

    private final AwsConfigurationClient awsConfigurationClient = new AwsConfigurationClient();

    private String cachedAwsAccountNo;

    public String getMeshMailBoxID() {
        return awsConfigurationClient.getParamValue(String.format("/repo/%s/user-input/external/mesh-mailbox-id", getEnvironmentName()));
    }

    public String getMeshMailBoxClientCert() {
        return awsConfigurationClient.getParamValue(String.format("/repo/%s/user-input/external/mesh-mailbox-client-cert", getEnvironmentName()));
    }

    public String getMeshMailBoxClientKey() {
        return awsConfigurationClient.getParamValue(String.format("/repo/%s/user-input/external/mesh-mailbox-client-key", getEnvironmentName()));
    }

    public String getMeshMailBoxPassword() {
        return awsConfigurationClient.getParamValue(String.format("/repo/%s/user-input/external/mesh-mailbox-password", getEnvironmentName()));
    }

    public String getPdsAdaptorApiKey() {
        return awsConfigurationClient.getParamValue(String.format("/repo/%s/user-input/api-keys/pds-adaptor/e2e-test", getEnvironmentName()));
    }

    public String getPdsAdaptorTestPatientNhsNumber() {
        return awsConfigurationClient.getParamValue(String.format("/repo/%s/user-input/external/e2e-test/pds-adaptor-test/nhs-number", getEnvironmentName()));
    }

    public String meshForwarderObservabilityQueueUri() {
        return String.format("https://sqs.eu-west-2.amazonaws.com/%s/%s-mesh-forwarder-nems-events-observability-queue", getAwsAccountNo(), getEnvironmentName());
    }

    public String nemsEventProcesorUnhandledQueueUri() {
        return String.format("https://sqs.eu-west-2.amazonaws.com/%s/%s-nems-event-processor-unhandled-events-queue", getAwsAccountNo(), getEnvironmentName());
    }

    public String suspensionsObservabilityQueueUri() {
        return String.format("https://sqs.eu-west-2.amazonaws.com/%s/%s-nems-event-processor-suspensions-observability-queue", getAwsAccountNo(), getEnvironmentName());
    }

    public String notReallySuspendedObservabilityQueueUri() {
        return String.format("https://sqs.eu-west-2.amazonaws.com/%s/%s-suspension-service-not-suspended-observability-queue", getAwsAccountNo(), getEnvironmentName());
    }

    public String NemsEventProcessorDeadLetterQueue() {
        return String.format("https://sqs.eu-west-2.amazonaws.com/%s/%s-nems-event-processor-dlq", getAwsAccountNo(), getEnvironmentName());
    }

    public String mofUpdatedQueueUri() {
        return String.format("https://sqs.eu-west-2.amazonaws.com/%s/%s-suspension-service-mof-updated-queue", getAwsAccountNo(), getEnvironmentName());
    }

    public String mofNotUpdatedQueueUri() {
        return String.format("https://sqs.eu-west-2.amazonaws.com/%s/%s-suspension-service-mof-not-updated-queue", getAwsAccountNo(), getEnvironmentName());
    }

    public String getPdsAdaptorUrl() {
        return String.format("https://pds-adaptor.%s.non-prod.patient-deductions.nhs.uk/", getEnvironmentName());
    }

    private String getAwsAccountNo() {
        if (cachedAwsAccountNo == null) {
            cachedAwsAccountNo = fetchAwsAccountNo();
        }
        return cachedAwsAccountNo;
    }

    private String fetchAwsAccountNo() {
        var client = StsClient.create();
        var response = client.getCallerIdentity();
        return response.account();
    }

    private String getEnvironmentName() {
        return getRequiredEnvVar("NHS_ENVIRONMENT");
    }

    private String getRequiredEnvVar(String name) {
        String value = System.getenv(name);
        if (value == null) {
            throw new RuntimeException("Required environment variable has not been set: " + name);
        }
        return value;
    }

    public List<String> suspendedNhsNumbers() {
        return suspendedNhsNumbersByEnv.get(getEnvironmentName());
    }
}
