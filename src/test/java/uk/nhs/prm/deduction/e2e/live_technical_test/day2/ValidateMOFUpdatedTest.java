package uk.nhs.prm.deduction.e2e.live_technical_test.day2;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.scheduling.annotation.EnableScheduling;
import uk.nhs.prm.deduction.e2e.TestConfiguration;
import uk.nhs.prm.deduction.e2e.live_technical_test.helpers.TestPatientValidator;
import uk.nhs.prm.deduction.e2e.pdsadaptor.PdsAdaptorClient;
import uk.nhs.prm.deduction.e2e.pdsadaptor.PdsAdaptorResponse;
import uk.nhs.prm.deduction.e2e.performance.awsauth.AssumeRoleCredentialsProviderFactory;
import uk.nhs.prm.deduction.e2e.performance.awsauth.AutoRefreshingRoleAssumingSqsClient;
import uk.nhs.prm.deduction.e2e.queue.SqsQueue;
import uk.nhs.prm.deduction.e2e.suspensions.MofUpdatedMessageQueue;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.nhs.prm.deduction.e2e.live_technical_test.TestParameters.fetchTestParameter;


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnableScheduling
public class ValidateMOFUpdatedTest {

    private MofUpdatedMessageQueue mofUpdatedMessageQueue;
    private TestConfiguration config = new TestConfiguration();
    private TestPatientValidator patientValidator = new TestPatientValidator();

    @BeforeEach
    public void setUp() {
        var sqsClient = new AutoRefreshingRoleAssumingSqsClient(new AssumeRoleCredentialsProviderFactory());
        mofUpdatedMessageQueue = new MofUpdatedMessageQueue(new SqsQueue(sqsClient), new TestConfiguration());
    }

    @Test
    public void shouldMoveSingleSuspensionMessageFromMeshMailBoxToNemsIncomingQueue() {
        var testPatientNhsNumber = fetchTestParameter("LIVE_TECHNICAL_TEST_NHS_NUMBER");
        var testPatientPreviousGp = fetchTestParameter("LIVE_TECHNICAL_TEST_PREVIOUS_GP");
        var expectedNemsMessageId = fetchTestParameter("LIVE_TECHNICAL_TEST_NEMS_MESSAGE_ID");

        // NEEDS UPDATING FOR TECHNICAL TEST USER IN LIVE XXXXX
        String pdsAdaptorUsernameXXX = "live-test";
        assertThat(isSafeListedOrSynthetic(testPatientNhsNumber)).isTrue();
        var patientStatus = fetchPdsPatientStatus(pdsAdaptorUsernameXXX, testPatientNhsNumber);
        assertThat(patientStatus.getManagingOrganisation()).isEqualTo(testPatientPreviousGp);

        var messageInQueue = mofUpdatedMessageQueue.getMessageContaining(expectedNemsMessageId);
        assertThat(messageInQueue.nemsMessageId()).isEqualTo(expectedNemsMessageId);
        assertThat(messageInQueue).isNotNull();
    }

    private PdsAdaptorResponse fetchPdsPatientStatus(String pdsAdaptorUsername, String testPatientNhsNumber) {
        var config = new TestConfiguration();
        var pds = new PdsAdaptorClient(pdsAdaptorUsername, config.getPdsAdaptorLiveTestApiKey(), config.getPdsAdaptorUrl());

        return pds.getSuspendedPatientStatus(testPatientNhsNumber);
    }

    private boolean isSafeListedOrSynthetic(String testPatientNhsNumber) {
        return patientValidator.isIncludedInTheTest(testPatientNhsNumber, config.getSafeListedPatientList(), config.getSyntheticPatientPrefix());
    }
}
