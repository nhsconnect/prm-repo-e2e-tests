package uk.nhs.prm.e2etests.live_technical_test.day2;

import org.springframework.test.context.TestPropertySource;
import uk.nhs.prm.e2etests.service.Gp2GpMessengerService;
import uk.nhs.prm.e2etests.property.Gp2gpMessengerProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.nhs.prm.e2etests.property.NhsProperties;
import uk.nhs.prm.e2etests.model.response.PdsAdaptorResponse;
import uk.nhs.prm.e2etests.service.PdsAdaptorService;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;


@SpringBootTest
@TestPropertySource(properties = {"test.pds.username=live-test"})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ValidateMOFUpdatedTest {

    private final String gp2GpMessengerApiKey;
    private final String gp2GpMessengerUrl;
    private final List<String> safeListedPatientList;
    private Gp2GpMessengerService gp2GpMessengerService;
    private final PdsAdaptorService pdsAdaptorService;

    @Autowired
    public ValidateMOFUpdatedTest(
            Gp2gpMessengerProperties gp2GpMessengerProperties,
            NhsProperties nhsProperties,
            PdsAdaptorService pdsAdaptorService
    ) {
        gp2GpMessengerApiKey = gp2GpMessengerProperties.getLiveTestApiKey();
        gp2GpMessengerUrl = gp2GpMessengerProperties.getGp2gpMessengerUrl();
        safeListedPatientList = nhsProperties.getSafeListedPatientList();
        this.pdsAdaptorService = pdsAdaptorService;
    }

    @BeforeEach
    void setUp() {
        gp2GpMessengerService = new Gp2GpMessengerService(gp2GpMessengerApiKey, gp2GpMessengerUrl);
    }

    @Test
    void shouldMoveSingleSuspensionMessageFromMeshMailBoxToNemsIncomingQueue() {
        if (safeListedPatientList.size() > 0) {
            System.out.println("Safe list patient has size " + safeListedPatientList.size()); // TODO PRMT-3574 refactor to a consistent logging approach

            safeListedPatientList.forEach(nhsNumber -> {
                var pdsResponse = fetchPdsPatientStatus(nhsNumber);
                System.out.println("Patient suspended status is:" + pdsResponse.getIsSuspended());

                System.out.println("Checking patient status with hl7 pds request - see logs for more details");
                gp2GpMessengerService.getPdsRecordViaHl7v3(nhsNumber);
            });
        }
    }

    private PdsAdaptorResponse fetchPdsPatientStatus(String testPatientNhsNumber) {
        return pdsAdaptorService.getSuspendedPatientStatus(testPatientNhsNumber);
    }
}
