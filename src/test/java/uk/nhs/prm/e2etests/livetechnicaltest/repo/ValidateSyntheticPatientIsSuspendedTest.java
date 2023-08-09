package uk.nhs.prm.e2etests.livetechnicaltest.repo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import uk.nhs.prm.e2etests.livetechnicaltest.TestParameters;
import uk.nhs.prm.e2etests.livetechnicaltest.helper.TestPatientValidator;
import uk.nhs.prm.e2etests.model.response.PdsAdaptorResponse;
import uk.nhs.prm.e2etests.service.PdsAdaptorService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@TestPropertySource(properties = {"test.pds.username=live-test"})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ValidateSyntheticPatientIsSuspendedTest {

    private final TestPatientValidator patientValidator;
    private final PdsAdaptorService pdsAdaptorService;

    @Autowired
    public ValidateSyntheticPatientIsSuspendedTest(
            TestPatientValidator testPatientValidator,
            PdsAdaptorService pdsAdaptorService
    ) {
        patientValidator = testPatientValidator;
        this.pdsAdaptorService = pdsAdaptorService;
    }

    @Test
    void shouldUpdateMofToRepoAndSuccessfullyRetrieveEhrFromPreviousPractise() {
        String testPatientNhsNumber = TestParameters.fetchTestParameter("LIVE_TECHNICAL_TEST_NHS_NUMBER");
        assertTrue(patientValidator.isIncludedInTheTest(testPatientNhsNumber));

        PdsAdaptorResponse pdsResponse = pdsAdaptorService.getSuspendedPatientStatus(testPatientNhsNumber);
        assertThat(pdsResponse.getIsSuspended()).as("patient status should be suspended").isTrue();
    }
}
