package uk.nhs.prm.e2etests.livetechnicaltest.day1;

import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import uk.nhs.prm.e2etests.livetechnicaltest.TestParameters;
import uk.nhs.prm.e2etests.livetechnicaltest.helper.TestPatientValidator;
import uk.nhs.prm.e2etests.model.SqsMessage;
import uk.nhs.prm.e2etests.model.response.PdsAdaptorResponse;
import uk.nhs.prm.e2etests.property.NhsProperties;
import uk.nhs.prm.e2etests.queue.suspensions.SuspensionServiceSuspensionsQueue;
import uk.nhs.prm.e2etests.service.Gp2GpMessengerService;
import uk.nhs.prm.e2etests.service.PdsAdaptorService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Log4j2
@SpringBootTest
@TestPropertySource(properties = {"test.pds.username=live-test"})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ChangeOfGPMessageReceivedTest {
    private final SuspensionServiceSuspensionsQueue suspensionServiceSuspensionsQueue;
    private final TestPatientValidator patientValidator;
    private final Gp2GpMessengerService gp2GpMessengerService;
    private final NhsProperties nhsProperties;
    private final PdsAdaptorService pdsAdaptorService;

    @Autowired
    public ChangeOfGPMessageReceivedTest(
            TestPatientValidator testPatientValidator,
            NhsProperties nhsProperties,
            PdsAdaptorService pdsAdaptorService,
            Gp2GpMessengerService gp2GpMessengerService,
            SuspensionServiceSuspensionsQueue suspensionServiceSuspensionsQueue
    ) {
        this.patientValidator = testPatientValidator;
        this.nhsProperties = nhsProperties;
        this.pdsAdaptorService = pdsAdaptorService;
        this.gp2GpMessengerService = gp2GpMessengerService;
        this.suspensionServiceSuspensionsQueue = suspensionServiceSuspensionsQueue;
    }

    @Test
    void shouldHaveReceivedSingleSuspensionChangeOfGpMessageRelatedToTestPatient() {
        List<String> safeListPatients = nhsProperties.getSafeListedPatientList();
        log.info("Safe listed patients: {}", safeListPatients);

        if (!safeListPatients.isEmpty()) {
            log.info("The list of safe listed patients has a size of: {}.", safeListPatients.size());

            safeListPatients.forEach(nhsNumber -> {
                log.info("Checking if the NHS number is synthetic.");
                assertTrue(patientValidator.isIncludedInTheTest(nhsNumber));

                PdsAdaptorResponse pdsResponse = getPatientStatusOnPDSForSyntheticPatient(nhsNumber);
                log.info("The current patient's suspended status is: {}.", pdsResponse.getIsSuspended());

                log.info("Checking the patient's status on HL7v3 PDS lookup, reference GP2GP Messenger logs for insights.");
//                gp2GpMessengerService.getPdsRecordViaHl7v3(nhsNumber);

                log.info("Finding related message for NHS number: {}.", nhsNumber);
                Optional<SqsMessage> suspensionMessage = suspensionServiceSuspensionsQueue.getMessageContainingForTechnicalTestRun(nhsNumber);
                assertTrue(suspensionMessage.isPresent());

                log.info("Found message related to test patient with NHS number: {}", nhsNumber);
                TestParameters.outputTestParameter("live_technical_test_nems_message_id", suspensionMessage.get().getNemsMessageId());
            });
        } else {
           log.warn("No safe list patients found for test.");
        }
    }

    private PdsAdaptorResponse getPatientStatusOnPDSForSyntheticPatient(String testPatientNhsNumber) {
        return pdsAdaptorService.getSuspendedPatientStatus(testPatientNhsNumber);
    }
}
