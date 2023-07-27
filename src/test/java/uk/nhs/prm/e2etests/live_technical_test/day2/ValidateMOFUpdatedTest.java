package uk.nhs.prm.e2etests.live_technical_test.day2;

import uk.nhs.prm.e2etests.services.gp2gp_messenger.Gp2GpMessengerClient;
import uk.nhs.prm.e2etests.property.Gp2gpMessengerProperties;
import uk.nhs.prm.e2etests.property.PdsAdaptorProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.nhs.prm.e2etests.property.NhsProperties;
import uk.nhs.prm.e2etests.pdsadaptor.PdsAdaptorResponse;
import uk.nhs.prm.e2etests.pdsadaptor.PdsAdaptorClient;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;


@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ValidateMOFUpdatedTest {

    private final String gp2GpMessengerApiKey;
    private final String gp2GpMessengerUrl;
    private final List<String> safeListedPatientList;
    private final String pdsAdaptorApiKey;
    private final String pdsAdaptorUrl;
    private String pdsAdaptorUsername = "live-test";
    private Gp2GpMessengerClient gp2GpMessengerClient;

    @Autowired
    public ValidateMOFUpdatedTest(
            Gp2gpMessengerProperties gp2GpMessengerProperties,
            PdsAdaptorProperties pdsAdaptorProperties,
            NhsProperties nhsProperties
    ) {
        gp2GpMessengerApiKey = gp2GpMessengerProperties.getLiveTestApiKey();
        gp2GpMessengerUrl = gp2GpMessengerProperties.getGp2gpMessengerUrl();
        safeListedPatientList = nhsProperties.getSafeListedPatientList();
        pdsAdaptorApiKey = pdsAdaptorProperties.getLiveTestApiKey();
        pdsAdaptorUrl = pdsAdaptorProperties.getPdsAdaptorUrl();
    }

    @BeforeEach
    void setUp() {
        gp2GpMessengerClient = new Gp2GpMessengerClient(gp2GpMessengerApiKey, gp2GpMessengerUrl);
    }

    @Test
    void shouldMoveSingleSuspensionMessageFromMeshMailBoxToNemsIncomingQueue() {
        if (safeListedPatientList.size() > 0) {
            System.out.println("Safe list patient has size " + safeListedPatientList.size()); // TODO PRMT-3574 refactor to a consistent logging approach

            safeListedPatientList.forEach(nhsNumber -> {
                var pdsResponse = fetchPdsPatientStatus(pdsAdaptorUsername, nhsNumber);
                System.out.println("Patient suspended status is:" + pdsResponse.getIsSuspended());

                System.out.println("Checking patient status with hl7 pds request - see logs for more details");
                gp2GpMessengerClient.getPdsRecordViaHlv7(nhsNumber);
            });
        }
    }

    // TODO: PRMT-3523 @TestPropertySource(properties = {"PDS_ADAPTOR_TEST_USERNAME = live-test", apiKey = something}) could be useful.
    private PdsAdaptorResponse fetchPdsPatientStatus(String pdsAdaptorUsername, String testPatientNhsNumber) {
        PdsAdaptorClient pds = new PdsAdaptorClient(pdsAdaptorUsername, pdsAdaptorApiKey, pdsAdaptorUrl);
        return pds.getSuspendedPatientStatus(testPatientNhsNumber);
    }
}
