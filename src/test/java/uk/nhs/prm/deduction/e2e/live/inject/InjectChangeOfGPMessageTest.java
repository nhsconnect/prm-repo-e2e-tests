package uk.nhs.prm.deduction.e2e.live.inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.annotation.EnableScheduling;
import uk.nhs.prm.deduction.e2e.TestConfiguration;
import uk.nhs.prm.deduction.e2e.mesh.MeshMailbox;
import uk.nhs.prm.deduction.e2e.nems.NemsEventMessage;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static java.time.ZoneOffset.ofHours;
import static java.time.ZonedDateTime.now;
import static uk.nhs.prm.deduction.e2e.nhs.NhsIdentityGenerator.generateRandomOdsCode;
import static uk.nhs.prm.deduction.e2e.nhs.NhsIdentityGenerator.randomNemsMessageId;
import static uk.nhs.prm.deduction.e2e.tests.EndToEndTest.SYNTHETIC_PATIENT_WHICH_HAS_CURRENT_GP_NHS_NUMBER;
import static uk.nhs.prm.deduction.e2e.utility.NemsEventFactory.createNemsEventFromTemplate;

@SpringBootTest(classes = {
        MeshMailbox.class,
        TestConfiguration.class,
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnableScheduling
public class InjectChangeOfGPMessageTest {

    @Autowired
    private MeshMailbox meshMailbox;

    @Test
    public void shouldMoveSingleSuspensionMessageFromMeshMailBoxToNemsIncomingQueue() {
        var nemsSuspension = createNemsEventFromTemplate(
                "change-of-gp-suspension.xml",
                SYNTHETIC_PATIENT_WHICH_HAS_CURRENT_GP_NHS_NUMBER,
                randomNemsMessageId(),
                generateRandomOdsCode(),
                now(ofHours(0)).toString());

        meshMailbox.postMessage(nemsSuspension);
    }
}
