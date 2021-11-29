package uk.nhs.prm.deduction.e2e;


import org.springframework.stereotype.Component;
import uk.nhs.prm.deduction.e2e.client.AwsConfigurationClient;

@Component
public class TestConfiguration {

    private AwsConfigurationClient awsConfigurationClient = new AwsConfigurationClient();

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

    private String getAwsAccountNo() {
        return getRequiredEnvVar("AWS_ACCOUNT_ID");
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
}
