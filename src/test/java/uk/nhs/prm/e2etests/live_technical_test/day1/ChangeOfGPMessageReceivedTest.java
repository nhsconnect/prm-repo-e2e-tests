package uk.nhs.prm.e2etests.live_technical_test.day1;

import uk.nhs.prm.e2etests.property.Gp2gpMessengerProperties;
import uk.nhs.prm.e2etests.property.NhsProperties;
import uk.nhs.prm.e2etests.property.PdsAdaptorProperties;
import uk.nhs.prm.e2etests.property.QueueProperties;
import uk.nhs.prm.e2etests.live_technical_test.helpers.TestPatientValidator;
import uk.nhs.prm.e2etests.service.Gp2GpMessengerService;
import uk.nhs.prm.e2etests.queue.suspensions.SuspensionServiceSuspensionsQueue;
import uk.nhs.prm.e2etests.live_technical_test.TestParameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.nhs.prm.e2etests.model.response.PdsAdaptorResponse;
import uk.nhs.prm.e2etests.service.PdsAdaptorService;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.nhs.prm.e2etests.service.SqsService;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ChangeOfGPMessageReceivedTest {
    private SuspensionServiceSuspensionsQueue suspensionServiceSuspensionsQueue;
    private TestPatientValidator patientValidator;
    private Gp2GpMessengerService gp2GpMessengerService;
    private Gp2gpMessengerProperties gp2GpMessengerProperties;
    private QueueProperties queueProperties;
    private NhsProperties nhsProperties;
    private PdsAdaptorProperties pdsAdaptorProperties;

    private SqsService sqsService;

    @Autowired
    public ChangeOfGPMessageReceivedTest(
            TestPatientValidator testPatientValidator,
            Gp2gpMessengerProperties gp2GpMessengerProperties,
            QueueProperties queueProperties,
            NhsProperties nhsProperties,
            PdsAdaptorProperties pdsAdaptorProperties,
            SqsService sqsService
    ) {
        this.patientValidator = testPatientValidator;
        this.gp2GpMessengerProperties = gp2GpMessengerProperties;
        this.queueProperties = queueProperties;
        this.nhsProperties = nhsProperties;
        this.pdsAdaptorProperties = pdsAdaptorProperties;
        this.sqsService = sqsService;
    }

    @BeforeEach
    public void setUp() {
        suspensionServiceSuspensionsQueue = new SuspensionServiceSuspensionsQueue(sqsService, queueProperties);
        gp2GpMessengerService = new Gp2GpMessengerService(gp2GpMessengerProperties.getLiveTestApiKey(), gp2GpMessengerProperties.getGp2gpMessengerUrl());
    }

    @Test
    void shouldHaveReceivedSingleSuspensionChangeOfGpMessageRelatedToTestPatient() {
        var safeListPatients = nhsProperties.getSafeListedPatientList();

        if (safeListPatients.size() > 0) {

            System.out.println("Safe list patient has size " + safeListPatients.size());

            safeListPatients.forEach(nhsNumber -> {
                System.out.println("Checking if nhs number is synthetic");
                assertThat(patientValidator.isIncludedInTheTest(nhsNumber)).isTrue();

                var pdsResponse = getPatientStatusOnPDSForSyntheticPatient(nhsNumber);

                System.out.println("Patient suspended status is:" + pdsResponse.getIsSuspended());

                System.out.println("Checking patient status on HL7 PDs lookup - see gp2gp messenger logs for insights");
                gp2GpMessengerService.getPdsRecordViaHl7v3(nhsNumber);

                System.out.println("Finding related message for nhs number");

                var suspensionMessage = suspensionServiceSuspensionsQueue.getMessageContainingForTechnicalTestRun(nhsNumber);

                System.out.println("got message related to test patient");
                TestParameters.outputTestParameter("live_technical_test_nems_message_id", suspensionMessage.getNemsMessageId());
            });


        } else {
            System.out.println("No safe list patients for test");
        }

    }

    private PdsAdaptorResponse getPatientStatusOnPDSForSyntheticPatient(String testPatientNhsNumber) {
        String pdsAdaptorUsername = "live-test";
        return fetchPdsPatientStatus(pdsAdaptorUsername, testPatientNhsNumber);
    }

    private PdsAdaptorResponse fetchPdsPatientStatus(String pdsAdaptorUsername, String testPatientNhsNumber) {

        var pds = new PdsAdaptorService(
                pdsAdaptorUsername,
                pdsAdaptorProperties.getLiveTestApiKey(),
                pdsAdaptorProperties.getPdsAdaptorUrl()
        );

        return pds.getSuspendedPatientStatus(testPatientNhsNumber);
    }
}
