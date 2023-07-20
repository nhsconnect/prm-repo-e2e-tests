package uk.nhs.prm.e2etests.live_technical_test.day2;

import uk.nhs.prm.e2etests.services.gp2gp_messenger.Gp2GpMessengerClient;
import uk.nhs.prm.e2etests.configuration.Gp2gpMessengerPropertySource;
import uk.nhs.prm.e2etests.configuration.PdsAdaptorPropertySource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.nhs.prm.e2etests.configuration.NhsPropertySource;
import uk.nhs.prm.e2etests.pdsadaptor.PdsAdaptorResponse;
import uk.nhs.prm.e2etests.pdsadaptor.PdsAdaptorClient;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;



@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnableScheduling
public class ValidateMOFUpdatedTest {

    private TestConfiguration config = new TestConfiguration();
    private String pdsAdaptorUsername = "live-test";
    private Gp2GpMessengerClient gp2GpMessengerClient;

    @BeforeEach
    void setUp() {
        gp2GpMessengerClient = new Gp2GpMessengerClient(config.getGp2GpMessengerApiKey(), config.getGp2GpMessengerUrl());
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

    private PdsAdaptorResponse fetchPdsPatientStatus(String pdsAdaptorUsername, String testPatientNhsNumber) {
        var config = new TestConfiguration();
        var pds = new PdsAdaptorClient(pdsAdaptorUsername, config.getPdsAdaptorLiveTestApiKey(), config.getPdsAdaptorUrl());

        return pds.getSuspendedPatientStatus(testPatientNhsNumber);
    }
}
