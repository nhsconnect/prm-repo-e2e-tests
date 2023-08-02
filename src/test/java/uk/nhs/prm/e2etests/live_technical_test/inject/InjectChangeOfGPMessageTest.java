package uk.nhs.prm.e2etests.live_technical_test.inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.annotation.EnableScheduling;
import uk.nhs.prm.e2etests.live_technical_test.TestParameters;
import uk.nhs.prm.e2etests.mesh.MeshMailbox;
import uk.nhs.prm.e2etests.model.nems.NemsEventMessage;
import uk.nhs.prm.e2etests.property.SyntheticPatientProperties;
import uk.nhs.prm.e2etests.queue.nems.observability.NemsEventProcessorSuspensionsOQ;

import static java.time.ZoneOffset.ofHours;
import static java.time.ZonedDateTime.now;
import static uk.nhs.prm.e2etests.utility.NhsIdentityGenerator.randomOdsCode;
import static uk.nhs.prm.e2etests.utility.NhsIdentityGenerator.randomNemsMessageId;
import static uk.nhs.prm.e2etests.utility.NemsEventFactory.createNemsEventFromTemplate;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnableScheduling
public class InjectChangeOfGPMessageTest {

    private MeshMailbox meshMailbox;
    private SyntheticPatientProperties syntheticPatientProperties;
    private NemsEventProcessorSuspensionsOQ nemsEventProcessorSuspensionsOQ;

    @Autowired
    public InjectChangeOfGPMessageTest(
            MeshMailbox meshMailbox,
            SyntheticPatientProperties syntheticPatientProperties,
            NemsEventProcessorSuspensionsOQ nemsEventProcessorSuspensionsOQ
    ) {
        this.meshMailbox = meshMailbox;
        this.syntheticPatientProperties = syntheticPatientProperties;
        this.nemsEventProcessorSuspensionsOQ = nemsEventProcessorSuspensionsOQ;
    }

    @Test
    public void shouldInjectTestMessageOnlyIntendedToRunInNonProdEnvironment() {
        String nemsMessageId = randomNemsMessageId();
        String nhsNumber = syntheticPatientProperties.getSyntheticPatientInPreProd();
        String previousGP = randomOdsCode();

        nemsEventProcessorSuspensionsOQ.deleteAllMessages();

        NemsEventMessage nemsSuspension = createNemsEventFromTemplate(
                "change-of-gp-suspension.xml",
                nhsNumber,
                nemsMessageId,
                previousGP,
                now(ofHours(0)).toString());

        meshMailbox.postMessage(nemsSuspension);

        // MAYBE add some extra synthetic patient change of gps to simulate UK-wide synthetic activity??

        System.out.println("injected nemsMessageId: " + nemsMessageId + " of course this will not be known in prod, so should be picked up when change of gp received");
        TestParameters.outputTestParameter("live_technical_test_nhs_number", nhsNumber);
        TestParameters.outputTestParameter("live_technical_test_previous_gp", previousGP);
    }
}
