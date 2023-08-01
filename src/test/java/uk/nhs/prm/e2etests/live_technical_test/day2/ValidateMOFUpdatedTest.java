package uk.nhs.prm.e2etests.live_technical_test.day2;

import org.springframework.test.context.TestPropertySource;
import uk.nhs.prm.e2etests.service.Gp2GpMessengerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.nhs.prm.e2etests.property.NhsProperties;
import uk.nhs.prm.e2etests.model.response.PdsAdaptorResponse;
import uk.nhs.prm.e2etests.service.PdsAdaptorService;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.Test;

import java.util.List;


@SpringBootTest
@TestPropertySource(properties = {"test.pds.username=live-test"})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ValidateMOFUpdatedTest {

    private final List<String> safeListedPatientList;
    private final Gp2GpMessengerService gp2GpMessengerService;
    private final PdsAdaptorService pdsAdaptorService;

    @Autowired
    public ValidateMOFUpdatedTest(
            NhsProperties nhsProperties,
            PdsAdaptorService pdsAdaptorService,
            Gp2GpMessengerService gp2GpMessengerService
    ) {
        safeListedPatientList = nhsProperties.getSafeListedPatientList();
        this.pdsAdaptorService = pdsAdaptorService;
        this.gp2GpMessengerService = gp2GpMessengerService;
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
