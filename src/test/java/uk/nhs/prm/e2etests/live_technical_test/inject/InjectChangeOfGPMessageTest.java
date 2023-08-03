package uk.nhs.prm.e2etests.live_technical_test.inject;

import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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

@Log4j2
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InjectChangeOfGPMessageTest {
    private final MeshMailbox meshMailbox;
    private final SyntheticPatientProperties syntheticPatientProperties;
    private final NemsEventProcessorSuspensionsOQ nemsEventProcessorSuspensionsOQ;

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
    void shouldInjectTestMessageOnlyIntendedToRunInNonProdEnvironment() {
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

        log.info("Injected NEMS Event Message ID: {}", nemsMessageId);
        TestParameters.outputTestParameter("live_technical_test_nhs_number", nhsNumber);
        TestParameters.outputTestParameter("live_technical_test_previous_gp", previousGP);
    }
}
