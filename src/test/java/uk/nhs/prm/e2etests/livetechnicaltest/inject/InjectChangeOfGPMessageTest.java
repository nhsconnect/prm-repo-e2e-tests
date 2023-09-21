package uk.nhs.prm.e2etests.livetechnicaltest.inject;

import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.nhs.prm.e2etests.enumeration.TemplateVariant;
import uk.nhs.prm.e2etests.livetechnicaltest.TestParameters;
import uk.nhs.prm.e2etests.mesh.MeshMailbox;
import uk.nhs.prm.e2etests.model.nems.NemsEventMessage;
import uk.nhs.prm.e2etests.property.SyntheticPatientProperties;
import uk.nhs.prm.e2etests.queue.nems.observability.NemsEventProcessorSuspensionsOQ;
import uk.nhs.prm.e2etests.service.TemplatingService;

import static java.time.ZoneOffset.ofHours;
import static java.time.ZonedDateTime.now;

import static uk.nhs.prm.e2etests.utility.TestDataUtility.randomNemsMessageId;
import static uk.nhs.prm.e2etests.utility.TestDataUtility.randomOdsCode;

@Log4j2
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InjectChangeOfGPMessageTest {
    private final MeshMailbox meshMailbox;
    private final SyntheticPatientProperties syntheticPatientProperties;
    private final NemsEventProcessorSuspensionsOQ nemsEventProcessorSuspensionsOQ;
    private final TemplatingService templatingService;

    @Autowired
    public InjectChangeOfGPMessageTest(
            MeshMailbox meshMailbox,
            SyntheticPatientProperties syntheticPatientProperties,
            NemsEventProcessorSuspensionsOQ nemsEventProcessorSuspensionsOQ,
            TemplatingService templatingService
    ) {
        this.meshMailbox = meshMailbox;
        this.syntheticPatientProperties = syntheticPatientProperties;
        this.nemsEventProcessorSuspensionsOQ = nemsEventProcessorSuspensionsOQ;
        this.templatingService = templatingService;
    }

    @Test
    void shouldInjectTestMessageOnlyIntendedToRunInNonProdEnvironment() {
        String nemsMessageId = randomNemsMessageId();
        String nhsNumber = syntheticPatientProperties.getSyntheticPatientInPreProd();
        String previousGP = randomOdsCode();

        nemsEventProcessorSuspensionsOQ.deleteAllMessages();

        NemsEventMessage nemsSuspension = templatingService.createNemsEventFromTemplate(
                TemplateVariant.CHANGE_OF_GP_SUSPENSION,
                nhsNumber,
                nemsMessageId,
                previousGP,
                now(ofHours(0)).toString());

        meshMailbox.sendMessage(nemsSuspension);

        log.info("Injected NEMS Event Message ID: {}", nemsMessageId);
        TestParameters.outputTestParameter("live_technical_test_nhs_number", nhsNumber);
        TestParameters.outputTestParameter("live_technical_test_previous_gp", previousGP);
    }
}
