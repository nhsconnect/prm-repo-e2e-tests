package uk.nhs.prm.deduction.e2e.live_technical_test.day1;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import uk.nhs.prm.e2etests.TestConfiguration;
import uk.nhs.prm.deduction.e2e.live_technical_test.helpers.TestPatientValidator;
import uk.nhs.prm.deduction.e2e.pdsadaptor.PdsAdaptorClient;
import uk.nhs.prm.deduction.e2e.pdsadaptor.PdsAdaptorResponse;
import uk.nhs.prm.e2etests.performance.awsauth.AssumeRoleCredentialsProviderFactory;
import uk.nhs.prm.e2etests.performance.awsauth.AutoRefreshingRoleAssumingSqsClient;
import uk.nhs.prm.deduction.e2e.queue.ThinlyWrappedSqsClient;
import uk.nhs.prm.e2etests.services.gp2gp_messenger.Gp2GpMessengerClient;
import uk.nhs.prm.deduction.e2e.suspensions.SuspensionMessageRealQueue;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.nhs.prm.deduction.e2e.live_technical_test.TestParameters.outputTestParameter;


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ChangeOfGPMessageReceivedTest {

    private SuspensionMessageRealQueue suspensionMessageRealQueue;
    private TestConfiguration config = new TestConfiguration();
    private TestPatientValidator patientValidator = new TestPatientValidator();
    private Gp2GpMessengerClient gp2GpMessengerClient;

    @BeforeEach
    public void setUp() {
        var sqsClient = new AutoRefreshingRoleAssumingSqsClient(new AssumeRoleCredentialsProviderFactory());
        suspensionMessageRealQueue = new SuspensionMessageRealQueue(new ThinlyWrappedSqsClient(sqsClient), config);
        gp2GpMessengerClient = new Gp2GpMessengerClient(config.getGp2GpMessengerApiKey(), config.getGp2GpMessengerUrl());
    }

    @Test
    public void shouldHaveReceivedSingleSuspensionChangeOfGpMessageRelatedToTestPatient() {
        var safeListPatients = Arrays.asList(config.getSafeListedPatientList().split(","));

        if (safeListPatients.size() > 0) {

            System.out.println("Safe list patient has size " + safeListPatients.size());

            safeListPatients.forEach(nhsNumber -> {
                System.out.println("Checking if nhs number is synthetic");
                assertThat(isSafeListedOrSynthetic(nhsNumber)).isTrue();

                var pdsResponse = getPatientStatusOnPDSForSyntheticPatient(nhsNumber);

                System.out.println("Patient suspended status is:" + pdsResponse.getIsSuspended());

                System.out.println("Checking patient status on HL7 PDs lookup - see gp2gp messenger logs for insights");
                gp2GpMessengerClient.getPdsRecordViaHlv7(nhsNumber);

                System.out.println("Finding related message for nhs number");

                var suspensionMessage = suspensionMessageRealQueue.getMessageContainingForTechnicalTestRun(nhsNumber);

                System.out.println("got message related to test patient");
                outputTestParameter("live_technical_test_nems_message_id", suspensionMessage.nemsMessageId());
            });


        } else {
            System.out.println("No safe list patients for test");
        }


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
