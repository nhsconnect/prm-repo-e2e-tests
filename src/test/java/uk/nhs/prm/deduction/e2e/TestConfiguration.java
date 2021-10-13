package uk.nhs.prm.deduction.e2e;

import org.springframework.beans.factory.annotation.Value;
import uk.nhs.prm.deduction.e2e.client.AwsConfigurationClient;


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

    private String getAwsAccountNo() {
        return System.getenv("AWS_ACCOUNT_ID");
    }

    private String getEnvironmentName() {
        return System.getenv("NHS_ENVIRONMENT");
    }
}
