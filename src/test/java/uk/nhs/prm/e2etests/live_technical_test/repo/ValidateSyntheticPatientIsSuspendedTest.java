package uk.nhs.prm.e2etests.live_technical_test.repo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.nhs.prm.e2etests.configuration.NhsPropertySource;
import uk.nhs.prm.e2etests.configuration.PdsAdaptorPropertySource;
import uk.nhs.prm.e2etests.live_technical_test.TestParameters;
import uk.nhs.prm.e2etests.live_technical_test.helpers.TestPatientValidator;
import uk.nhs.prm.e2etests.pdsadaptor.PdsAdaptorClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ValidateSyntheticPatientIsSuspendedTest {

    private final TestConfiguration config = new TestConfiguration();
    private static final String PDS_ADAPTOR_TEST_USERNAME = "live-test";
    private PdsAdaptorClient pdsAdaptorClient;
    private TestPatientValidator patientValidator = new TestPatientValidator();

    @BeforeEach
    void setUp() {
        pdsAdaptorClient = new PdsAdaptorClient(PDS_ADAPTOR_TEST_USERNAME, config.getPdsAdaptorLiveTestApiKey(), config.getPdsAdaptorUrl());
    }

    @Test
    void shouldUpdateMofToRepoAndSuccessfullyRetrieveEhrFromPreviousPractise() {
        var testPatientNhsNumber = TestParameters.fetchTestParameter("LIVE_TECHNICAL_TEST_NHS_NUMBER");
        assertThat(patientValidator.isIncludedInTheTest(testPatientNhsNumber)).isTrue();

        var pdsResponse = pdsAdaptorClient.getSuspendedPatientStatus(testPatientNhsNumber);
        assertThat(pdsResponse.getIsSuspended()).as("patient status should be suspended").isTrue();
    }

    private boolean isSafeListedOrSynthetic(String testPatientNhsNumber) {
        return patientValidator.isIncludedInTheTest(testPatientNhsNumber, config.getSafeListedPatientList(), config.getSyntheticPatientPrefix());
    }

}
