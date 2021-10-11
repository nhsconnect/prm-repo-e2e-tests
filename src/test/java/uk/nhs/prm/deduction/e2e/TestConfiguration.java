package uk.nhs.prm.deduction.e2e;

import uk.nhs.prm.deduction.e2e.client.AwsConfigurationClient;


public class TestConfiguration {

    private AwsConfigurationClient awsConfigurationClient = new AwsConfigurationClient();

    public String getMeshMailBoxID() {
        return awsConfigurationClient.getParaValue(String.format("/repo/%s/user-input/external/mesh-mailbox-id", getEnvironmentName()));
    }

    public String meshForwarderObservabilityQueueUri() {
        return String.format("https://sqs.eu-west-2.amazonaws.com/%s/%s-mesh-forwarder-nems-events-observability-queue", getAwsAccountNo(), getEnvironmentName());
    }

    private String getAwsAccountNo() {
        return System.getenv("AWS_ACCOUNT_NO");
    }

    private String getEnvironmentName() {
        return System.getenv("NHS_ENVIRONMENT");
    }
}
