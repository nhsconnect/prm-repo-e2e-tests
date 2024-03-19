package uk.nhs.prm.e2etests.livetechnicaltest.day2;

import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import uk.nhs.prm.e2etests.model.response.PdsAdaptorResponse;
import uk.nhs.prm.e2etests.property.NhsProperties;
import uk.nhs.prm.e2etests.service.Gp2GpMessengerService;
import uk.nhs.prm.e2etests.service.PdsAdaptorService;

import java.util.List;

@Log4j2
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
            log.info("The safe list of patients has size: {}.", safeListedPatientList.size());

            safeListedPatientList.forEach(nhsNumber -> {
                PdsAdaptorResponse pdsResponse = fetchPdsPatientStatus(nhsNumber);
                log.info("The patient's suspended status is: {}.", pdsResponse.getIsSuspended());

                log.info("Checking patient status with HL7v3 PDS request - reference logs for more details.");
//                gp2GpMessengerService.getPdsRecordViaHl7v3(nhsNumber);
            });
        }
    }

    private PdsAdaptorResponse fetchPdsPatientStatus(String testPatientNhsNumber) {
        return pdsAdaptorService.getSuspendedPatientStatus(testPatientNhsNumber);
    }
}
