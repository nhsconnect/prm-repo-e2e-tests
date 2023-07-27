package uk.nhs.prm.e2etests.live_technical_test.repo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.nhs.prm.e2etests.configuration.ExampleAssumedRoleArn;
import uk.nhs.prm.e2etests.property.NhsProperties;
import uk.nhs.prm.e2etests.property.PdsAdaptorProperties;
import uk.nhs.prm.e2etests.property.QueueProperties;
import uk.nhs.prm.e2etests.live_technical_test.TestParameters;
import uk.nhs.prm.e2etests.queue.ehr_transfer.RepoIncomingQueue;
import uk.nhs.prm.e2etests.live_technical_test.helpers.TestPatientValidator;
import uk.nhs.prm.e2etests.model.RepoIncomingMessageBuilder;
import uk.nhs.prm.e2etests.client.PdsAdaptorClient;
import uk.nhs.prm.e2etests.model.response.PdsAdaptorResponse;
import uk.nhs.prm.e2etests.client.EhrRepoClient;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ValidateSyntheticEhrTransferInToRepoUsingMofTest {

    private static final String PDS_ADAPTOR_TEST_USERNAME = "live-test";
    private TestPatientValidator patientValidator;
    private PdsAdaptorClient pdsAdaptorClient;
    private EhrRepoClient ehrRepoClient;
    private String repoOdsCode;
    private List<String> safeListedPatientList;
    private String syntheticPatientPrefix;
    private RepoIncomingQueue repoIncomingQueue;
    private QueueProperties queueProperties;
    private ExampleAssumedRoleArn exampleAssumedRoleArn;

    @Autowired
    public ValidateSyntheticEhrTransferInToRepoUsingMofTest(
            TestPatientValidator testPatientValidator,
            PdsAdaptorProperties pdsAdaptorProperties,
            NhsProperties nhsProperties,
            QueueProperties queueProperties,
            ExampleAssumedRoleArn exampleAssumedRoleArn,
            RepoIncomingQueue repoIncomingQueue
    ) {
        patientValidator = testPatientValidator;

        pdsAdaptorClient = new PdsAdaptorClient(
                PDS_ADAPTOR_TEST_USERNAME,
                pdsAdaptorProperties.getLiveTestApiKey(),
                pdsAdaptorProperties.getPdsAdaptorUrl());
        repoOdsCode = nhsProperties.getRepoOdsCode();
        safeListedPatientList = nhsProperties.getSafeListedPatientList();
        syntheticPatientPrefix = nhsProperties.getSyntheticPatientPrefix();
        this.queueProperties = queueProperties;
        this.exampleAssumedRoleArn = exampleAssumedRoleArn;
        this.repoIncomingQueue = repoIncomingQueue;
    }

    @Test
    void shouldUpdateMofToRepoAndSuccessfullyRetrieveEhrFromPreviousPractise() {
        var testPatientNhsNumber = TestParameters.fetchTestParameter("LIVE_TECHNICAL_TEST_NHS_NUMBER");
        var testPatientPreviousGp = TestParameters.fetchTestParameter("LIVE_TECHNICAL_TEST_PREVIOUS_GP");

        assertThat(patientValidator.isIncludedInTheTest(testPatientNhsNumber)).isTrue();
        updateMofToRepoOdsCode(testPatientNhsNumber);

        var triggerMessage = new RepoIncomingMessageBuilder()
                .withNhsNumber(testPatientNhsNumber)
                .withEhrSourceGpOdsCode(testPatientPreviousGp)
                .withEhrDestination(repoOdsCode)
                .build();

        repoIncomingQueue.send(triggerMessage);

        int timeout = 1;
        System.out.println("Checking ehr repo for " + timeout + " hours until health record is stored successfully");
        await().atMost(timeout, TimeUnit.HOURS).with().pollInterval(10, TimeUnit.SECONDS)
                .until(() -> ehrRepoClient.isPatientHealthRecordStatusComplete(testPatientNhsNumber, triggerMessage.conversationId()),
                        equalTo(true));

    }

    private void updateMofToRepoOdsCode(String testPatientNhsNumber) {
        PdsAdaptorResponse pdsResponse = getPdsAdaptorResponse(testPatientNhsNumber);

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
}
