package uk.nhs.prm.deduction.e2e.live_technical_test.repo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import uk.nhs.prm.deduction.e2e.TestConfiguration;
import uk.nhs.prm.deduction.e2e.live_technical_test.helpers.TestPatientValidator;
import uk.nhs.prm.deduction.e2e.pdsadaptor.PdsAdaptorClient;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.nhs.prm.deduction.e2e.live_technical_test.TestParameters.fetchTestParameter;

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
        var testPatientNhsNumber = fetchTestParameter("LIVE_TECHNICAL_TEST_NHS_NUMBER");

        assertThat(isSafeListedOrSynthetic(testPatientNhsNumber)).isTrue();

        var pdsResponse = pdsAdaptorClient.getSuspendedPatientStatus(testPatientNhsNumber);

        assertThat(pdsResponse.getIsSuspended()).as("patient status should be suspended").isTrue();
    }

    private boolean isSafeListedOrSynthetic(String testPatientNhsNumber) {
        return patientValidator.isIncludedInTheTest(testPatientNhsNumber, config.getSafeListedPatientList(), config.getSyntheticPatientPrefix());
    }

}
