package uk.nhs.prm.e2etests.live_technical_test.repo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.nhs.prm.e2etests.property.PdsAdaptorProperties;
import uk.nhs.prm.e2etests.live_technical_test.TestParameters;
import uk.nhs.prm.e2etests.live_technical_test.helpers.TestPatientValidator;
import uk.nhs.prm.e2etests.pdsadaptor.PdsAdaptorClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ValidateSyntheticPatientIsSuspendedTest {

    private final TestPatientValidator patientValidator;
    private final String pdsAdaptorApiKey;
    private final String pdsAdaptorUrl;
    private static final String PDS_ADAPTOR_TEST_USERNAME = "live-test";
    private PdsAdaptorClient pdsAdaptorClient;

    @Autowired
    public ValidateSyntheticPatientIsSuspendedTest(
            TestPatientValidator testPatientValidator,
            PdsAdaptorProperties pdsAdaptorProperties
    ) {
        patientValidator = testPatientValidator;
        pdsAdaptorApiKey = pdsAdaptorProperties.getLiveTestApiKey();
        pdsAdaptorUrl = pdsAdaptorProperties.getPdsAdaptorUrl();
    }

    // TODO: PRMT-3523 @TestPropertySource(properties = {"PDS_ADAPTOR_TEST_USERNAME = live-test", apiKey = something}) could be useful.
    @BeforeEach
    void setUp() {
        pdsAdaptorClient = new PdsAdaptorClient(PDS_ADAPTOR_TEST_USERNAME, pdsAdaptorApiKey, pdsAdaptorUrl);
    }

    @Test
    void shouldUpdateMofToRepoAndSuccessfullyRetrieveEhrFromPreviousPractise() {
        var testPatientNhsNumber = TestParameters.fetchTestParameter("LIVE_TECHNICAL_TEST_NHS_NUMBER");
        assertThat(patientValidator.isIncludedInTheTest(testPatientNhsNumber)).isTrue();

        var pdsResponse = pdsAdaptorClient.getSuspendedPatientStatus(testPatientNhsNumber);
        assertThat(pdsResponse.getIsSuspended()).as("patient status should be suspended").isTrue();
    }
}
