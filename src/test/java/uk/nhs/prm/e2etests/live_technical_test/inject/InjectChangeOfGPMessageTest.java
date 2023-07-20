package uk.nhs.prm.e2etests.live_technical_test.inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.annotation.EnableScheduling;
import uk.nhs.prm.e2etests.configuration.QueuePropertySource;
import uk.nhs.prm.e2etests.live_technical_test.TestParameters;
import uk.nhs.prm.e2etests.TestConfiguration;
import uk.nhs.prm.e2etests.mesh.MeshMailbox;
import uk.nhs.prm.e2etests.performance.awsauth.AssumeRoleCredentialsProviderFactory;
import uk.nhs.prm.e2etests.performance.awsauth.AutoRefreshingRoleAssumingSqsClient;
import uk.nhs.prm.e2etests.queue.ThinlyWrappedSqsClient;
import uk.nhs.prm.e2etests.suspensions.SuspensionMessageObservabilityQueue;

import static java.time.ZoneOffset.ofHours;
import static java.time.ZonedDateTime.now;
import static uk.nhs.prm.e2etests.nhs.NhsIdentityGenerator.generateRandomOdsCode;
import static uk.nhs.prm.e2etests.nhs.NhsIdentityGenerator.randomNemsMessageId;
import static uk.nhs.prm.e2etests.utility.NemsEventFactory.createNemsEventFromTemplate;

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
    private TestConfiguration testConfiguration;

    @Autowired
    private SuspensionMessageObservabilityQueue suspensionMessageObservabilityQueue;

    @BeforeEach
    public void setUp() {
        var sqsClient = new AutoRefreshingRoleAssumingSqsClient(new AssumeRoleCredentialsProviderFactory());
        suspensionMessageObservabilityQueue = new SuspensionMessageObservabilityQueue(new ThinlyWrappedSqsClient(sqsClient), queuePropertySource);
    }


    @Test
    public void shouldInjectTestMessageOnlyIntendedToRunInNonProdEnvironment() {
        String nemsMessageId = randomNemsMessageId();
        String nhsNumber = testConfiguration.getNhsNumberForSyntheticPatientInPreProd();
        String previousGP = generateRandomOdsCode();

        suspensionMessageObservabilityQueue.deleteAllMessages();

        var nemsSuspension = createNemsEventFromTemplate(
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
