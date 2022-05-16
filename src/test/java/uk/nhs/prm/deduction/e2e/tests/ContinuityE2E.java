package uk.nhs.prm.deduction.e2e.tests;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.nhs.prm.deduction.e2e.TestConfiguration;
import uk.nhs.prm.deduction.e2e.deadletter.NemsEventProcessorDeadLetterQueue;
import uk.nhs.prm.deduction.e2e.mesh.MeshMailbox;
import uk.nhs.prm.deduction.e2e.models.DeceasedPatientMessage;
import uk.nhs.prm.deduction.e2e.models.MofUpdatedMessage;
import uk.nhs.prm.deduction.e2e.models.NoLongerSuspendedMessage;
import uk.nhs.prm.deduction.e2e.models.ResolutionMessage;
import uk.nhs.prm.deduction.e2e.nems.MeshForwarderQueue;
import uk.nhs.prm.deduction.e2e.nems.NemsEventMessage;
import uk.nhs.prm.deduction.e2e.nems.NemsEventProcessorUnhandledQueue;
import uk.nhs.prm.deduction.e2e.performance.awsauth.AssumeRoleCredentialsProviderFactory;
import uk.nhs.prm.deduction.e2e.performance.awsauth.AutoRefreshingRoleAssumingSqsClient;
import uk.nhs.prm.deduction.e2e.queue.BasicSqsClient;
import uk.nhs.prm.deduction.e2e.queue.SqsQueue;
import uk.nhs.prm.deduction.e2e.suspensions.*;
import uk.nhs.prm.deduction.e2e.utility.NemsEventFactory;
import uk.nhs.prm.deduction.e2e.utility.QueueHelper;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static uk.nhs.prm.deduction.e2e.nhs.NhsIdentityGenerator.*;
import static uk.nhs.prm.deduction.e2e.utility.NemsEventFactory.createNemsEventFromTemplate;
import static java.lang.System.getenv;

@SpringBootTest(classes = {
        ContinuityE2E.class,
        MeshMailbox.class,
        SqsQueue.class,
        TestConfiguration.class,
        MeshForwarderQueue.class,
        NemsEventProcessorUnhandledQueue.class,
        SuspensionMessageObservabilityQueue.class,
        SuspensionServiceNotReallySuspensionsMessageQueue.class,
        NemsEventProcessorDeadLetterQueue.class,
        MeshForwarderQueue.class,
        QueueHelper.class,
        MofUpdatedMessageQueue.class,
        DeceasedPatientQueue.class,
        MofNotUpdatedMessageQueue.class,
        BasicSqsClient.class,
        AssumeRoleCredentialsProviderFactory.class,
        AutoRefreshingRoleAssumingSqsClient.class
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ContinuityE2E {

    @Autowired
    private MeshForwarderQueue meshForwarderQueue;
    @Autowired
    private NemsEventProcessorUnhandledQueue nemsEventProcessorUnhandledQueue;
    @Autowired
    private SuspensionMessageObservabilityQueue suspensionsMessageQueue;
    @Autowired
    private SuspensionServiceNotReallySuspensionsMessageQueue notReallySuspensionsMessageQueue;
    @Autowired
    private MofUpdatedMessageQueue mofUpdatedMessageQueue;
    @Autowired
    private MofNotUpdatedMessageQueue mofNotUpdatedMessageQueue;
    @Autowired
    private NemsEventProcessorDeadLetterQueue nemsEventProcessorDeadLetterQueue;
    @Autowired
    private DeceasedPatientQueue deceasedPatientQueue;
    @Autowired
    private MeshMailbox meshMailbox;
    @Autowired
    private TestConfiguration config;

    @BeforeAll
    void init() {
        meshForwarderQueue.deleteAllMessages();
        nemsEventProcessorDeadLetterQueue.deleteAllMessages();
        suspensionsMessageQueue.deleteAllMessages();
        nemsEventProcessorUnhandledQueue.deleteAllMessages();
        notReallySuspensionsMessageQueue.deleteAllMessages();
    }

    @Test
    @DisabledIfEnvironmentVariable(named = "UPDATE_MOF_TO_REPO",matches="true")
    @Order(1)
    public void shouldMoveSuspensionMessageFromNemsToMofUpdatedQueue() {
        String nemsMessageId = randomNemsMessageId();
        String suspendedPatientNhsNumber = config.getNhsNumberForSyntheticPatientWithoutGp();
        var now = ZonedDateTime.now(ZoneOffset.ofHours(0)).toString();
        String previousGp = generateRandomOdsCode();
        System.out.printf("Generated random ods code for previous gp: %s%n", previousGp);

        NemsEventMessage nemsSuspension = createNemsEventFromTemplate("change-of-gp-suspension.xml", suspendedPatientNhsNumber, nemsMessageId, previousGp, now);
        meshMailbox.postMessage(nemsSuspension);
        MofUpdatedMessage expectedMessageOnQueue = new MofUpdatedMessage(nemsMessageId, "ACTION:UPDATED_MANAGING_ORGANISATION");

        assertThat(meshForwarderQueue.hasMessage(nemsSuspension.body()));
        assertThat(mofUpdatedMessageQueue.hasResolutionMessage(expectedMessageOnQueue));
    }


    @Test
    @Order(2)
    public void shouldMoveSuspensionMessageWherePatientIsNoLongerSuspendedToNotSuspendedQueue() {
        String nemsMessageId = randomNemsMessageId();
        String previousGp = generateRandomOdsCode();
        var now = ZonedDateTime.now(ZoneOffset.ofHours(0)).toString();
        String currentlyRegisteredPatientNhsNumber = config.getNhsNumberForSyntheticPatientWithCurrentGp();

        NemsEventMessage nemsSuspension = createNemsEventFromTemplate("change-of-gp-suspension.xml", currentlyRegisteredPatientNhsNumber, nemsMessageId, previousGp,now);

        NoLongerSuspendedMessage expectedMessageOnQueue = new NoLongerSuspendedMessage(nemsMessageId, "NO_ACTION:NO_LONGER_SUSPENDED_ON_PDS");

        meshMailbox.postMessage(nemsSuspension);
        assertThat(notReallySuspensionsMessageQueue.hasResolutionMessage(expectedMessageOnQueue));

    }

    @Test
    @Order(5)
    public void shouldMoveNonSuspensionMessageFromNemsToUnhandledQueue() throws Exception {
        String nemsMessageId = randomNemsMessageId();
        String nhsNumber = randomNhsNumber();
        var now = ZonedDateTime.now(ZoneOffset.ofHours(0)).toString();
        NemsEventMessage nemsNonSuspension = createNemsEventFromTemplate("change-of-gp-non-suspension.xml", nhsNumber, nemsMessageId,now);
        meshMailbox.postMessage(nemsNonSuspension);
        assertThat(nemsEventProcessorUnhandledQueue.hasMessage("{\"nemsMessageId\":\"" + nemsMessageId + "\",\"messageStatus\":\"NO_ACTION:NON_SUSPENSION\"}"));
    }


    @Test
    @Order(6)
    public void shouldSendUnprocessableMessagesToDlQ() throws Exception {
        Map<String, NemsEventMessage> dlqMessages = NemsEventFactory.getDLQNemsEventMessages();
        log("Posting DLQ messages");
        for (Map.Entry<String, NemsEventMessage> message : dlqMessages.entrySet()) {
            meshMailbox.postMessage(message.getValue());
            log("Posted " + message.getKey() + " messages");
            assertThat(nemsEventProcessorDeadLetterQueue.hasMessage(message.getValue().body()));
        }
    }

    @Test
    @Order(4)
    public void shouldMoveNonSyntheticPatientSuspensionMessageFromNemsToMofNotUpdatedQueueWhenToggleOn() {
        String nemsMessageId = randomNemsMessageId();
        String previousGp = generateRandomOdsCode();
        var now = ZonedDateTime.now(ZoneOffset.ofHours(0)).toString();
        var nemsSuspension = createNemsEventFromTemplate("change-of-gp-suspension.xml",
                config.getNhsNumberForNonSyntheticPatientWithoutGp(),
                nemsMessageId, previousGp,now);

        meshMailbox.postMessage(nemsSuspension);

        ResolutionMessage expectedMessageOnQueue = new ResolutionMessage(nemsMessageId, "NO_ACTION:NOT_SYNTHETIC_OR_SAFE_LISTED");

        assertThat(mofNotUpdatedMessageQueue.hasResolutionMessage(expectedMessageOnQueue));
    }

    @Test
    @Order(3)
    public void shouldMoveDeceasedPatientToDeceasedQueue() {
        String nemsMessageId = randomNemsMessageId();
        String suspendedPatientNhsNumber = config.getNhsNumberForSyntheticDeceasedPatient();
        var now = ZonedDateTime.now(ZoneOffset.ofHours(0)).toString();
        String previousGp = generateRandomOdsCode();
        System.out.printf("Generated random ods code for previous gp: %s%n", previousGp);

        NemsEventMessage nemsSuspension = createNemsEventFromTemplate("change-of-gp-suspension.xml", suspendedPatientNhsNumber, nemsMessageId, previousGp,now);
        meshMailbox.postMessage(nemsSuspension);
        DeceasedPatientMessage expectedMessageOnQueue = new DeceasedPatientMessage(nemsMessageId, "NO_ACTION:DECEASED_PATIENT");

        assertThat(deceasedPatientQueue.hasResolutionMessage(expectedMessageOnQueue));
    }
    public void log(String message) {
        System.out.println(message);
    }
}
