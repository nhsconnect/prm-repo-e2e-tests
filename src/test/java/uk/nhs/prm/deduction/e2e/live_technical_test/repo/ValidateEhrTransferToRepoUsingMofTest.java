package uk.nhs.prm.deduction.e2e.live_technical_test.repo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import uk.nhs.prm.deduction.e2e.TestConfiguration;
import uk.nhs.prm.deduction.e2e.services.ehr_repo.EhrRepoClient;
import uk.nhs.prm.deduction.e2e.services.gp2gp_messenger.Gp2GpMessengerClient;
import uk.nhs.prm.deduction.e2e.pdsadaptor.PdsAdaptorClient;
import uk.nhs.prm.deduction.e2e.pdsadaptor.PdsAdaptorResponse;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static uk.nhs.prm.deduction.e2e.live_technical_test.TestParameters.fetchTestParameter;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ValidateEhrTransferToRepoUsingMofTest {

    private final TestConfiguration config = new TestConfiguration();
    private static final String PDS_ADAPTOR_TEST_USERNAME = "live-test";
    private PdsAdaptorClient pdsAdaptorClient;
    private Gp2GpMessengerClient gp2GpMessengerClient;
    private EhrRepoClient ehrRepoClient;

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

        validatePatientIsSynthetic(testPatientNhsNumber);
        updateMofToRepoOdsCode(testPatientNhsNumber);

        var conversationId = UUID.randomUUID().toString();
        System.out.println("Generated conversation id " + conversationId);

        isHealthRecordRequestSuccessful(testPatientNhsNumber, testPatientPreviousGp, conversationId);

        System.out.println("Checking ehr repo for 60s till health record is stored is successfully");
        await().atMost(60, TimeUnit.SECONDS).with().pollInterval(5, TimeUnit.SECONDS)
                .until(() -> ehrRepoClient.isPatientHealthRecordStatusComplete(testPatientNhsNumber, conversationId),
                        equalTo(true));

    }

    private void validatePatientIsSynthetic(String testPatientNhsNumber) {
        System.out.println("Checking if nhs number is synthetic");
        assertThat(testPatientNhsNumber).startsWith(config.getSyntheticPatientPrefix());
    }

    private void updateMofToRepoOdsCode(String testPatientNhsNumber) {
        PdsAdaptorResponse pdsResponse = getPdsAdaptorResponse(testPatientNhsNumber);
        var repoOdsCode = config.getRepoOdsCode();
        var updatedMofResponse = pdsAdaptorClient.updateManagingOrganisation(testPatientNhsNumber, repoOdsCode, pdsResponse.getRecordETag());
        System.out.println("Confirming patient managing organisation is set to repo ods code");
        assertThat(updatedMofResponse.getManagingOrganisation()).isEqualTo(repoOdsCode);
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


}
