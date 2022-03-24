package uk.nhs.prm.deduction.e2e.live.inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.annotation.EnableScheduling;
import uk.nhs.prm.deduction.e2e.TestConfiguration;
import uk.nhs.prm.deduction.e2e.mesh.MeshMailbox;
import uk.nhs.prm.deduction.e2e.utility.Files;

import static java.time.ZoneOffset.ofHours;
import static java.time.ZonedDateTime.now;
import static uk.nhs.prm.deduction.e2e.nhs.NhsIdentityGenerator.generateRandomOdsCode;
import static uk.nhs.prm.deduction.e2e.nhs.NhsIdentityGenerator.randomNemsMessageId;
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

    @Autowired
    private TestConfiguration config;

    @Test
    public void shouldInjectTestMessageOnlyIntendedToRunInNonProdEnvironment() {
        String nemsMessageId = randomNemsMessageId();
        String nhsNumber = config.getNhsNumberForSyntheticPatientWithoutGp();
        String previousGP = generateRandomOdsCode();

        var nemsSuspension = createNemsEventFromTemplate(
                "change-of-gp-suspension.xml",
                nhsNumber,
                nemsMessageId,
                previousGP,
                now(ofHours(0)).toString());

        meshMailbox.postMessage(nemsSuspension);

        System.out.println("injected nemsMessageId: " + nemsMessageId + " of course this will not be known in prod, so should be picked up when change of gp received");
        Files.outputTestData("live_technical_test_nhs_number", nhsNumber);
        Files.outputTestData("live_technical_test_previous_gp", previousGP);
    }
}
