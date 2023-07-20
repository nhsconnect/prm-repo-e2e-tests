package uk.nhs.prm.e2etests;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.utils.ImmutableMap;
import uk.nhs.prm.e2etests.configuration.BootstrapConfiguration;
import uk.nhs.prm.deduction.e2e.performance.load.LoadPhase;
import uk.nhs.prm.deduction.e2e.performance.load.LoadSpecParser;
import uk.nhs.prm.e2etests.services.SsmService;

import java.util.List;

import static java.lang.Integer.parseInt;
import static java.lang.System.getenv;

@Component
public class TestConfiguration {
    // INSTANCE VARIABLES
    public static final int SECONDS_IN_AN_HOUR = 3600;
    private final ImmutableMap<String, List<String>> suspendedNhsNumbersByEnv = ImmutableMap.of(
            "dev", TestData.dev(),
            "pre-prod", TestData.preProd(),
            "perf", TestData.perf(numberOfPerfNhsNumbers())
    );

    // BEANS
    private final ExampleAssumedRoleArn exampleAssumedRoleArn;

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

    private final SsmService ssmService;

    @Autowired
    public TestConfiguration(
            ExampleAssumedRoleArn exampleAssumedRoleArn,
            SsmService ssmService
    ) {
        this.ssmService = ssmService;
        this.exampleAssumedRoleArn = exampleAssumedRoleArn;
    }

    public String getTransferTrackerDb() {
        return getEnvironmentName() + "-ehr-transfer-service-transfer-tracker";
    }

    public String getActiveSuspensionsDb() {
        return getEnvironmentName() + "-re-registration-service-active-suspensions";
    }

    public String getSyntheticPatientPrefix() {
        return getEnvironmentName().equals("prod") ? "999" : "969";
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

    private String getEnvSuffix() {
        return getEnvironmentName().equals("prod") ? "prod" : String.format("%s.non-prod", getEnvironmentName());
    }

    public String endOfTransferMofUpdatedQueue() {
        return getQueueUri("end-of-transfer-service-mof-updated");
    }

    public String smallEhrQueueUri() {
        return getQueueUri("ehr-transfer-service-small-ehr-observability");
    }
    public String ehrInUnhandledQueueUri() {
        return getQueueUri("ehr-transfer-service-unhandled-observability");
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

    public String gp2gpMessengerQueueUri() { return getQueueUri("hl7-message-sent-observability"); }

    public String getMqUserName() {
        return awsConfiguration.getParamValue(String.format("/repo/%s/user-input/mq-app-username", getEnvironmentName()));
    }

    public String getMqPassword() {
        return awsConfiguration.getParamValue(String.format("/repo/%s/user-input/mq-app-password", getEnvironmentName()));
    }

    public String fragmentQueueUri() {
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
