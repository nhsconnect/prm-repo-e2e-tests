package uk.nhs.prm.deduction.e2e.live_technical_test.day1;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import uk.nhs.prm.deduction.e2e.TestConfiguration;
import uk.nhs.prm.deduction.e2e.live_technical_test.helpers.TestPatientValidator;
import uk.nhs.prm.deduction.e2e.pdsadaptor.PdsAdaptorClient;
import uk.nhs.prm.deduction.e2e.pdsadaptor.PdsAdaptorResponse;
import uk.nhs.prm.deduction.e2e.performance.awsauth.AssumeRoleCredentialsProviderFactory;
import uk.nhs.prm.deduction.e2e.performance.awsauth.AutoRefreshingRoleAssumingSqsClient;
import uk.nhs.prm.deduction.e2e.queue.SqsQueue;
import uk.nhs.prm.deduction.e2e.suspensions.SuspensionMessageRealQueue;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.nhs.prm.deduction.e2e.live_technical_test.TestParameters.fetchTestParameter;
import static uk.nhs.prm.deduction.e2e.live_technical_test.TestParameters.outputTestParameter;


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ChangeOfGPMessageReceivedTest {

    private SuspensionMessageRealQueue suspensionMessageRealQueue;
    private TestConfiguration config = new TestConfiguration();
    private TestPatientValidator patientValidator = new TestPatientValidator();

    @BeforeEach
    public void setUp() {
        var sqsClient = new AutoRefreshingRoleAssumingSqsClient(new AssumeRoleCredentialsProviderFactory());
        suspensionMessageRealQueue = new SuspensionMessageRealQueue(new SqsQueue(sqsClient), config);
    }

    @Test
    public void shouldHaveReceivedSingleSuspensionChangeOfGpMessageRelatedToTestPatient() {
        var testPatientNhsNumber = fetchTestParameter("LIVE_TECHNICAL_TEST_NHS_NUMBER");
        var testPatientPreviousGp = fetchTestParameter("LIVE_TECHNICAL_TEST_PREVIOUS_GP");

        System.out.println("Checking if nhs number is synthetic");
        assertThat(isSafeListedOrSynthetic(testPatientNhsNumber)).isTrue();

        var pdsResponse = getPatientStatusOnPDSForSyntheticPatient(testPatientNhsNumber);

        assertThat(pdsResponse.getIsSuspended()).isTrue();

        System.out.println("expecting test nhs number and previous gp of: " + testPatientNhsNumber + ", " + testPatientPreviousGp);

        var suspensionMessage = suspensionMessageRealQueue.getMessageContainingForTechnicalTestRun(testPatientNhsNumber);

        System.out.println("got message related to test patient");
        outputTestParameter("live_technical_test_nems_message_id", suspensionMessage.nemsMessageId());

        assertThat(suspensionMessage.previousGp()).isEqualTo(testPatientPreviousGp);

    }

    private boolean isSafeListedOrSynthetic(String testPatientNhsNumber) {
        return patientValidator.isIncludedInTheTest(testPatientNhsNumber, config.getSafeListedPatientList(), config.getSyntheticPatientPrefix());
    }

    private PdsAdaptorResponse getPatientStatusOnPDSForSyntheticPatient(String testPatientNhsNumber) {
        String pdsAdaptorUsernameXXX = "live-test";
        return fetchPdsPatientStatus(pdsAdaptorUsernameXXX, testPatientNhsNumber);
    }

    private PdsAdaptorResponse fetchPdsPatientStatus(String pdsAdaptorUsername, String testPatientNhsNumber) {

        var pds = new PdsAdaptorClient(pdsAdaptorUsername, config.getPdsAdaptorLiveTestApiKey(), config.getPdsAdaptorUrl());

        return pds.getSuspendedPatientStatus(testPatientNhsNumber);
    }

}
