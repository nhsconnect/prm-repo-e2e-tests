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

@Component
public class TestConfiguration {

    public static final int SECONDS_IN_AN_HOUR = 3600;

    private final ImmutableMap<String, List<String>> suspendedNhsNumbersByEnv = ImmutableMap.of(
            "dev", TestData.dev(),
            "pre-prod", TestData.preProd(),
            "perf", TestData.perf(numberOfPerfNhsNumbers())
    );

    public String getNhsNumberForSyntheticPatientWithCurrentGp() {
        return getEnvironmentName().equals("dev") ? "9693796284" : "9694179254";
    }

    public String getNhsNumberForSyntheticPatientWithoutGp() {
        return getEnvironmentName().equals("dev") ? "9693795997" : "9694179343";
    }

    public String getNhsNumberForSyntheticDeceasedPatient() {
        return getEnvironmentName().equals("dev") ? "9693797264" : "9694179394";
    }

    public String getNhsNumberForSyntheticPatientInPreProd() {
        return "9693642422";
    }

    public String getNhsNumberForNonSyntheticPatientWithoutGp() {
        return "9692295400";
    }

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

    public String getPdsAdaptorPerformanceApiKey() {
        return awsConfigurationClient.getParamValue(String.format("/repo/%s/user-input/api-keys/pds-adaptor/performance-test", getEnvironmentName()));
    }

    public String getPdsAdaptorLiveTestApiKey() {
        return awsConfigurationClient.getParamValue(String.format("/repo/%s/user-input/api-keys/pds-adaptor/live-test", getEnvironmentName()));
    }

    public String getPdsAdaptorE2ETestApiKey() {
        return awsConfigurationClient.getParamValue(String.format("/repo/%s/user-input/api-keys/pds-adaptor/e2e-test", getEnvironmentName()));
    }

    public String getGp2GpMessengerApiKey() {
        return awsConfigurationClient.getParamValue(String.format("/repo/%s/user-input/api-keys/gp2gp-messenger/live-test", getEnvironmentName()));
    }

    public String getEhrRepoApiKey() {
        return awsConfigurationClient.getParamValue(String.format("/repo/%s/user-input/api-keys/ehr-repo/live-test", getEnvironmentName()));
    }

    public String getEhrRepoE2EApiKey() {
        return awsConfigurationClient.getParamValue(String.format("/repo/%s/user-input/api-keys/ehr-repo/e2e-test", getEnvironmentName()));
    }

    public String getRepoOdsCode() {
        return awsConfigurationClient.getParamValue(String.format("/repo/%s/user-input/external/repository-ods-code", getEnvironmentName()));
    }

    public String getRepoAsid() {
        return awsConfigurationClient.getParamValue(String.format("/repo/%s/user-input/external/repository-asid", getEnvironmentName()));
    }

    public String meshForwarderObservabilityQueueUri() {
        return getQueueUri("mesh-forwarder-nems-events-observability");
    }

    public String nemsEventProcesorUnhandledQueueUri() {
        return getQueueUri("nems-event-processor-unhandled-events");
    }

    public String suspensionsObservabilityQueueUri() {
        return getQueueUri("nems-event-processor-suspensions-observability");
    }

    public String reRegistrationObservabilityQueueUri() {
        return getQueueUri("nems-event-processor-re-registration-observability");
    }

    public String suspensionsRealQueueUri() {
        return getQueueUri("suspension-service-suspensions");
    }

    public String notReallySuspendedObservabilityQueueUri() {
        return getQueueUri("suspension-service-not-suspended-observability");
    }

    public String nemsEventProcessorDeadLetterQueue() {
        return getQueueUri("nems-event-processor-dlq");
    }

    public String mofUpdatedQueueUri() {
        return getQueueUri("suspension-service-mof-updated");
    }

    public String mofNotUpdatedQueueUri() {
        return getQueueUri("suspension-service-mof-not-updated");
    }

    public String deceasedQueueUri() {
        return getQueueUri("suspension-service-deceased-patient");
    }

    public String repoIncomingQueueUri() {
        return getQueueUri("ehr-transfer-service-repo-incoming");
    }

    public String repoIncomingObservabilityQueueUri() {
        return getQueueUri("suspension-service-repo-incoming-observability");
    }

    public String getTransferTrackerDb() {
        return getEnvironmentName() + "-ehr-transfer-service-transfer-tracker";
    }

    public String getActiveSupensionsDetailsDb() {
        return getEnvironmentName() + "-re-registration-service-active-suspensions-details-dynamodb";
    }

    public String getSyntheticPatientPrefix() {
        return getEnvironmentName().equals("prod") ? "999" : "969";
    }

    public String getSafeListedPatientList() {
        return awsConfigurationClient.getParamValue(String.format("/repo/%s/user-input/external/safe-listed-patients-nhs-numbers", getEnvironmentName()));
    }

    private String getQueueUri(String name) {
        return String.format("https://sqs.eu-west-2.amazonaws.com/%s/%s-%s", getAwsAccountNo(), getEnvironmentName(), name);
    }

    public String getPdsAdaptorUrl() {
        return String.format("https://pds-adaptor.%s.patient-deductions.nhs.uk/", getEnvSuffix());
    }

    public String getGp2GpMessengerUrl() {
        return String.format("https://gp2gp-messenger.%s.patient-deductions.nhs.uk/", getEnvSuffix());
    }

    public String getEhrRepoUrl() {
        return String.format("https://ehr-repo.%s.patient-deductions.nhs.uk/", getEnvSuffix());
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

    private int numberOfPerfNhsNumbers() {
        String perfPatientsRequested = getenv("NUMBER_OF_PERF_NHS_NUMBERS");
        if (perfPatientsRequested == null) {
            return 40;
        }
        return parseInt(perfPatientsRequested);
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

    public String getEnvironmentName() {
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

    private String getEnvSuffix() {
        return getEnvironmentName().equals("prod") ? "prod" : String.format("%s.non-prod", getEnvironmentName());
    }

    public String endOfTransferMofUpdatedQueue() {
        return getQueueUri("end-of-transfer-service-mof-updated");
    }

    public String smallEhrQueueUri() {
        return getQueueUri("ehr-transfer-service-small-ehr-observability");
    }

    public String negativeAcknowledgementQueueUri() {
        return getQueueUri("ehr-transfer-service-negative-acknowledgments-observability");
    }

    public String ehrCompleteQueueUri() {
        return getQueueUri("ehr-transfer-service-ehr-complete-observability");
    }

    public String transferCompleteQueueUri() {
        return getQueueUri("end-of-transfer-service-transfer-complete-observability");
    }

    public String largeEhrQueueUri() {
        return getQueueUri("ehr-transfer-service-large-ehr-observability");
    }

    public String getMqUserName() {
        return awsConfigurationClient.getParamValue(String.format("/repo/%s/user-input/mq-app-username", getEnvironmentName()));
    }

    public String getMqPassword() {
        return awsConfigurationClient.getParamValue(String.format("/repo/%s/user-input/mq-app-password", getEnvironmentName()));
    }

    public String attachmentQueueUri() {
        return getQueueUri("ehr-transfer-service-large-message-fragments-observability");
    }

    public String parsingDLQ() {
        return getQueueUri("ehr-transfer-service-parsing-dlq");
    }

    public String getAmqpEndpoint1() {
        var devEndpoint = "b-09f25472-2c58-4386-ad2c-675ce15efbd6-1.mq.eu-west-2.amazonaws.com";
        var perfEndpoint = "b-b7552552-9d19-43a6-91a7-677285f32b04-1.mq.eu-west-2.amazonaws.com";
        return getEnvironmentName().equals("dev") ? devEndpoint : perfEndpoint;
    }
}
