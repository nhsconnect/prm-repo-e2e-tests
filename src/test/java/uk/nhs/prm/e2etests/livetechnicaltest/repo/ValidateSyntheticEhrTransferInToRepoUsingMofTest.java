package uk.nhs.prm.e2etests.livetechnicaltest.repo;

import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import uk.nhs.prm.e2etests.livetechnicaltest.TestParameters;
import uk.nhs.prm.e2etests.livetechnicaltest.helper.TestPatientValidator;
import uk.nhs.prm.e2etests.model.RepoIncomingMessage;
import uk.nhs.prm.e2etests.model.RepoIncomingMessageBuilder;
import uk.nhs.prm.e2etests.model.response.PdsAdaptorResponse;
import uk.nhs.prm.e2etests.property.NhsProperties;
import uk.nhs.prm.e2etests.queue.ehrtransfer.EhrTransferServiceRepoIncomingQueue;
import uk.nhs.prm.e2etests.service.EhrRepositoryService;
import uk.nhs.prm.e2etests.service.PdsAdaptorService;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Log4j2
@SpringBootTest
@TestPropertySource(properties = {"test.pds.username=live-test"})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ValidateSyntheticEhrTransferInToRepoUsingMofTest {
    private final TestPatientValidator patientValidator;
    private final PdsAdaptorService pdsAdaptorService;
    private final EhrRepositoryService ehrRepositoryService;
    private final String repoOdsCode;
    private final EhrTransferServiceRepoIncomingQueue ehrTransferServiceRepoIncomingQueue;

    @Autowired
    public ValidateSyntheticEhrTransferInToRepoUsingMofTest(
            TestPatientValidator testPatientValidator,
            PdsAdaptorService pdsAdaptorService,
            EhrRepositoryService ehrRepositoryService,
            NhsProperties nhsProperties,
            EhrTransferServiceRepoIncomingQueue ehrTransferServiceRepoIncomingQueue
    ) {
        patientValidator = testPatientValidator;

        this.pdsAdaptorService = pdsAdaptorService;
        this.ehrRepositoryService = ehrRepositoryService;
        repoOdsCode = nhsProperties.getRepoOdsCode();
        this.ehrTransferServiceRepoIncomingQueue = ehrTransferServiceRepoIncomingQueue;
    }

    @Test
    void shouldUpdateMofToRepoAndSuccessfullyRetrieveEhrFromPreviousPractise() {
        String testPatientNhsNumber = TestParameters.fetchTestParameter("LIVE_TECHNICAL_TEST_NHS_NUMBER");
        String testPatientPreviousGp = TestParameters.fetchTestParameter("LIVE_TECHNICAL_TEST_PREVIOUS_GP");

        assertTrue(patientValidator.isIncludedInTheTest(testPatientNhsNumber));
        updateMofToRepoOdsCode(testPatientNhsNumber);

        RepoIncomingMessage triggerMessage = new RepoIncomingMessageBuilder()
                .withNhsNumber(testPatientNhsNumber)
                .withEhrSourceGpOdsCode(testPatientPreviousGp)
                .withEhrDestination(repoOdsCode)
                .build();

        ehrTransferServiceRepoIncomingQueue.send(triggerMessage);

        int timeout = 1;
        log.info("Checking EHR Repository for {} hours until health record is stored successfully.", timeout);
        await().atMost(timeout, TimeUnit.HOURS).with().pollInterval(10, TimeUnit.SECONDS)
                .until(() -> ehrRepositoryService.isPatientHealthRecordStatusComplete(testPatientNhsNumber, triggerMessage.getConversationId()),
                        equalTo(true));

    }

    private void updateMofToRepoOdsCode(String testPatientNhsNumber) {
        PdsAdaptorResponse pdsResponse = getPdsAdaptorResponse(testPatientNhsNumber);
        if (repoOdsCode.equals(pdsResponse.getManagingOrganisation())) {
            log.info("Not sending update request because MOF already set to EHR Repository ODS code.");
        } else {
            PdsAdaptorResponse updatedMofResponse = pdsAdaptorService.updateManagingOrganisation(testPatientNhsNumber, repoOdsCode, pdsResponse.getRecordETag());
            log.info("Confirming that the patient MOD is set to EHR Repository ODS code.");
            assertThat(updatedMofResponse.getManagingOrganisation()).isEqualTo(repoOdsCode);
        }
    }

    private PdsAdaptorResponse getPdsAdaptorResponse(String testPatientNhsNumber) {
        PdsAdaptorResponse pdsResponse = pdsAdaptorService.getSuspendedPatientStatus(testPatientNhsNumber);
        log.info("Confirming that the patient's status is suspended.");
        assertTrue(pdsResponse.getIsSuspended());
        return pdsResponse;
    }
}
