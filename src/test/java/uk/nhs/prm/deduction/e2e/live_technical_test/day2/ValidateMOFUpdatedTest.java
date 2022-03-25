package uk.nhs.prm.deduction.e2e.live_technical_test.day2;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.scheduling.annotation.EnableScheduling;
import uk.nhs.prm.deduction.e2e.TestConfiguration;
import uk.nhs.prm.deduction.e2e.pdsadaptor.PdsAdaptorClient;
import uk.nhs.prm.deduction.e2e.pdsadaptor.PdsAdaptorResponse;
import uk.nhs.prm.deduction.e2e.performance.awsauth.AssumeRoleCredentialsProviderFactory;
import uk.nhs.prm.deduction.e2e.performance.awsauth.AutoRefreshingRoleAssumingSqsClient;
import uk.nhs.prm.deduction.e2e.queue.SqsMessage;
import uk.nhs.prm.deduction.e2e.queue.SqsQueue;
import uk.nhs.prm.deduction.e2e.suspensions.MofUpdatedMessageQueue;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.nhs.prm.deduction.e2e.live_technical_test.TestParameters.fetchTestParameter;


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnableScheduling
public class ValidateMOFUpdatedTest {

    private MofUpdatedMessageQueue mofUpdatedMessageQueue;

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

        System.out.println("expecting test nhs number, previous gp and nems message id of: " + testPatientNhsNumber + ", " + testPatientPreviousGp + ", " + expectedNemsMessageId);

        // NEEDS UPDATING FOR TECHNICAL TEST USER IN LIVE XXXXX
        String pdsAdaptorUsernameXXX = "live-test";

        var patientStatus = fetchPdsPatientStatus(pdsAdaptorUsernameXXX, testPatientNhsNumber);

        assertThat(patientStatus.getManagingOrganisation()).isEqualTo(testPatientPreviousGp);

        var messagesForPatient = getMessagesMatchingNhsNumber(expectedNemsMessageId);

        assertThat(messagesForPatient.size()).isGreaterThan(0);

        // check on messages, e.g. presence of nems message id

    }

    private List<SqsMessage> getMessagesMatchingNhsNumber(String nemsMessageId) {
        var messagesForPatient = new ArrayList<SqsMessage>();
        SqsMessage matchingMessage;
        do {
            matchingMessage = mofUpdatedMessageQueue.getMessageContaining(nemsMessageId);
            if (matchingMessage != null) {
                messagesForPatient.add(matchingMessage);
            }
        } while (matchingMessage != null);
        return messagesForPatient;
    }

    private PdsAdaptorResponse fetchPdsPatientStatus(String pdsAdaptorUsername, String testPatientNhsNumber) {
        var config = new TestConfiguration();
        var pds = new PdsAdaptorClient(pdsAdaptorUsername, config.getPdsAdaptorLiveTestApiKey(), config.getPdsAdaptorUrl());

        return pds.getSuspendedPatientStatus(testPatientNhsNumber);
    }

}
