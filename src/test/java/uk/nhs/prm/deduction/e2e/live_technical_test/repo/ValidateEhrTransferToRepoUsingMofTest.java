package uk.nhs.prm.deduction.e2e.live_technical_test.repo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import uk.nhs.prm.deduction.e2e.TestConfiguration;
import uk.nhs.prm.deduction.e2e.ehr_transfer.RepoIncomingQueue;
import uk.nhs.prm.deduction.e2e.live_technical_test.helpers.TestPatientValidator;
import uk.nhs.prm.deduction.e2e.models.Gp2GpSystem;
import uk.nhs.prm.deduction.e2e.models.RepoIncomingMessageBuilder;
import uk.nhs.prm.deduction.e2e.services.ehr_repo.EhrRepoClient;
import uk.nhs.prm.deduction.e2e.services.gp2gp_messenger.Gp2GpMessengerClient;
import uk.nhs.prm.deduction.e2e.pdsadaptor.PdsAdaptorClient;
import uk.nhs.prm.deduction.e2e.pdsadaptor.PdsAdaptorResponse;
import uk.nhs.prm.deduction.e2e.transfer_tracker_db.TrackerDb;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.nhs.prm.deduction.e2e.live_technical_test.TestParameters.fetchTestParameter;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ValidateEhrTransferToRepoUsingMofTest {

    private final TestConfiguration config = new TestConfiguration();
    private static final String PDS_ADAPTOR_TEST_USERNAME = "live-test";
    private PdsAdaptorClient pdsAdaptorClient;
    private Gp2GpMessengerClient gp2GpMessengerClient;
    private EhrRepoClient ehrRepoClient;
    private TestPatientValidator patientValidator = new TestPatientValidator();
    @Autowired
    TrackerDb trackerDb;
    @Autowired
    private RepoIncomingQueue repoIncomingQueue;
    @BeforeEach
    void setUp() {
        pdsAdaptorClient = new PdsAdaptorClient(PDS_ADAPTOR_TEST_USERNAME, config.getPdsAdaptorLiveTestApiKey(), config.getPdsAdaptorUrl());
        gp2GpMessengerClient = new Gp2GpMessengerClient(config.getGp2GpMessengerApiKey(), config.getGp2GpMessengerUrl());
        ehrRepoClient = new EhrRepoClient(config.getEhrRepoApiKey(), config.getEhrRepoUrl());
    }

    @Test
    void shouldUpdateMofToRepoAndSuccessfullyRetrieveEhrFromPreviousPractise() {
        var testPatientNhsNumber = fetchTestParameter("LIVE_TECHNICAL_TEST_NHS_NUMBER");
        var testPatientPreviousGp = fetchTestParameter("LIVE_TECHNICAL_TEST_PREVIOUS_GP");

        assertThat(isSafeListedOrSynthetic(testPatientNhsNumber)).isTrue();
        updateMofToRepoOdsCode(testPatientNhsNumber);

        var triggerMessage = new RepoIncomingMessageBuilder()
                .withNhsNumber(testPatientNhsNumber)
                .withEhrSourceGpOdsCode(testPatientPreviousGp)
                .withEhrDestination(config.getRepoOdsCode())
                .build();

        repoIncomingQueue.send(triggerMessage);

        System.out.println("Checking ehr repo for 60s till health record is stored is successfully");
        await().atMost(60, TimeUnit.SECONDS).with().pollInterval(5, TimeUnit.SECONDS)
                .until(() -> ehrRepoClient.isPatientHealthRecordStatusComplete(testPatientNhsNumber, triggerMessage.conversationId()),
                        equalTo(true));
        assertTrue(trackerDb.statusForConversationIdIs(triggerMessage.conversationId(), "ACTION:EHR_TRANSFER_TO_REPO_COMPLETE"));

    }

    private void updateMofToRepoOdsCode(String testPatientNhsNumber) {
        PdsAdaptorResponse pdsResponse = getPdsAdaptorResponse(testPatientNhsNumber);
        var repoOdsCode = config.getRepoOdsCode();
        if (repoOdsCode.equals(pdsResponse.getManagingOrganisation())) {
            System.out.println("Not sending update request because managing organisation already set to repo ods code");
        } else {
            var updatedMofResponse = pdsAdaptorClient.updateManagingOrganisation(testPatientNhsNumber, repoOdsCode, pdsResponse.getRecordETag());
            System.out.println("Confirming patient managing organisation is set to repo ods code");
            assertThat(updatedMofResponse.getManagingOrganisation()).isEqualTo(repoOdsCode);
        }
    }

    private PdsAdaptorResponse getPdsAdaptorResponse(String testPatientNhsNumber) {
        var pdsResponse = pdsAdaptorClient.getSuspendedPatientStatus(testPatientNhsNumber);
        System.out.println("Confirming patient status is suspended");
        assertThat(pdsResponse.getIsSuspended()).isTrue();
        return pdsResponse;
    }

    private void isHealthRecordRequestSuccessful(String testPatientNhsNumber, String testPatientPreviousGp, String conversationId) {
        var healthRecordRequestSentSuccessful = gp2GpMessengerClient.isHealthRecordRequestSentSuccessful(testPatientNhsNumber, config.getRepoOdsCode(), config.getRepoAsid(),
                testPatientPreviousGp, conversationId);
        assertThat(healthRecordRequestSentSuccessful).isTrue();
    }
    private boolean isSafeListedOrSynthetic(String testPatientNhsNumber) {
        return patientValidator.isIncludedInTheTest(testPatientNhsNumber, config.getSafeListedPatientList(), config.getSyntheticPatientPrefix());
    }

}
