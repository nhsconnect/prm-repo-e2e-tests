package uk.nhs.prm.deduction.e2e;


import org.springframework.stereotype.Component;
import software.amazon.awssdk.utils.ImmutableMap;
import uk.nhs.prm.deduction.e2e.client.AwsConfigurationClient;
import uk.nhs.prm.deduction.e2e.client.RoleAssumingAwsConfigurationClient;
import uk.nhs.prm.deduction.e2e.config.BootstrapConfiguration;
import uk.nhs.prm.deduction.e2e.performance.awsauth.AssumeRoleCredentialsProviderFactory;
import uk.nhs.prm.deduction.e2e.performance.load.LoadPhase;
import uk.nhs.prm.deduction.e2e.performance.load.LoadSpecParser;

import java.util.List;

import static java.lang.Integer.parseInt;
import static java.lang.System.getenv;
import static java.util.Arrays.asList;

@Component
public class TestConfiguration {

    public static final int SECONDS_IN_AN_HOUR = 3600;

    private final ImmutableMap<String, List<String>> suspendedNhsNumbersByEnv = ImmutableMap.of(
            "dev", asList(
                    "9693797396",
                    "9693797426",
                    "9693797477",
                    "9693797418",
                    "9693797361",
                    "9693797353",
                    "9693797345",
                    "9693797337",
                    "9693797329",
                    "9693797310",
                    "9693797469",
                    "9693797507",
                    "9693797434",
                    "9693797167",
                    "9693797159"),
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

    private final AwsConfigurationClient awsConfigurationClient;

    private volatile String cachedAwsAccountNo;

    public TestConfiguration() {
        this.awsConfigurationClient = createAwsConfigurationClient();
    }

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

    public String getPdsAdaptorPerformanceApiKey() {
        return awsConfigurationClient.getParamValue(String.format("/repo/%s/user-input/api-keys/pds-adaptor/performance-test", getEnvironmentName()));
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
        return BootstrapConfiguration.exampleAssumeRoleArn().accountNo();
    }

    public List<String> suspendedNhsNumbers() {
        List<String> nhsNumbers = suspendedNhsNumbersByEnv.get(getEnvironmentName());
        if (nhsNumbers == null) {
            throw new RuntimeException("No NHS Numbers configured for " + getEnvironmentName() + " environment");
        }
        return nhsNumbers;
    }

    public List<LoadPhase> performanceTestLoadPhases(List<LoadPhase> defaultLoadPhases) {
        String loadSpec = getenv("PERFORMANCE_LOAD_SPEC");
        if (loadSpec == null) {
            return defaultLoadPhases;
        }
        return LoadSpecParser.parsePhases(loadSpec);
    }

    public int performanceTestTimeout() {
        String timeout = getenv("PERFORMANCE_TEST_TIMEOUT");
        if (timeout == null) {
            return 90;
        }
        return parseInt(timeout);
    }

    private String getEnvironmentName() {
        return getRequiredEnvVar("NHS_ENVIRONMENT");
    }

    public static String getRequiredEnvVar(String name) {
        String value = getenv(name);
        if (value == null) {
            throw new RuntimeException("Required environment variable has not been set: " + name);
        }
        return value;
    }

    private AwsConfigurationClient createAwsConfigurationClient() {
        return new RoleAssumingAwsConfigurationClient(new AssumeRoleCredentialsProviderFactory());
    }
}
