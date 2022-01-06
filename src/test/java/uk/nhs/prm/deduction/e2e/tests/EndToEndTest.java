package uk.nhs.prm.deduction.e2e.tests;

import org.awaitility.core.ThrowingRunnable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import software.amazon.awssdk.services.sqs.model.Message;
import uk.nhs.prm.deduction.e2e.TestConfiguration;
import uk.nhs.prm.deduction.e2e.auth.AuthTokenGenerator;
import uk.nhs.prm.deduction.e2e.deadletter.NemsEventProcessorDeadLetterQueue;
import uk.nhs.prm.deduction.e2e.mesh.MeshClient;
import uk.nhs.prm.deduction.e2e.mesh.MeshMailbox;
import uk.nhs.prm.deduction.e2e.nems.MeshForwarderQueue;
import uk.nhs.prm.deduction.e2e.nems.NemsEventMessage;
import uk.nhs.prm.deduction.e2e.nems.NemsEventMessageQueue;
import uk.nhs.prm.deduction.e2e.nems.NemsEventProcessorUnhandledQueue;
import uk.nhs.prm.deduction.e2e.pdsadaptor.PdsAdaptorClient;
import uk.nhs.prm.deduction.e2e.pdsadaptor.PdsAdaptorResponse;
import uk.nhs.prm.deduction.e2e.queue.SqsQueue;
import uk.nhs.prm.deduction.e2e.suspensions.MofUpdatedMessageQueue;
import uk.nhs.prm.deduction.e2e.suspensions.NemsEventProcessorSuspensionsMessageQueue;
import uk.nhs.prm.deduction.e2e.suspensions.SuspensionServiceNotReallySuspensionsMessageQueue;
import uk.nhs.prm.deduction.e2e.utility.Helper;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


@SpringBootTest(classes = {
        EndToEndTest.class,
        MeshMailbox.class,
        SqsQueue.class,
        MeshClient.class,
        TestConfiguration.class,
        AuthTokenGenerator.class,
        MeshForwarderQueue.class,
        NemsEventProcessorUnhandledQueue.class,
        NemsEventProcessorSuspensionsMessageQueue.class,
        SuspensionServiceNotReallySuspensionsMessageQueue.class,
        NemsEventProcessorDeadLetterQueue.class,
        MeshForwarderQueue.class,
        Helper.class,
        MofUpdatedMessageQueue.class,
        PdsAdaptorClient.class
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class EndToEndTest {

    public static final String PATIENT_WHICH_HAS_CURRENT_GP_NHS_NUMBER = "9692294994";
    public static final String PATIENT_WHICH_HAS_NO_CURRENT_GP_NHS_NUMBER = "9693797515";
    @Autowired
    private MeshForwarderQueue meshForwarderQueue;
    @Autowired
    private NemsEventProcessorUnhandledQueue nemsEventProcessorUnhandledQueue;
    @Autowired
    private NemsEventProcessorSuspensionsMessageQueue suspensionsMessageQueue;
    @Autowired
    private SuspensionServiceNotReallySuspensionsMessageQueue notReallySuspensionsMessageQueue;
    @Autowired
    private MofUpdatedMessageQueue mofUpdatedMessageQueue;
    @Autowired
    private NemsEventProcessorDeadLetterQueue nemsEventProcessorDeadLetterQueue;
    @Autowired
    private MeshMailbox meshMailbox;
    @Autowired
    private Helper helper;

    @BeforeAll
    void init(){
        meshForwarderQueue.deleteAllMessages();
        nemsEventProcessorDeadLetterQueue.deleteAllMessages();
        suspensionsMessageQueue.deleteAllMessages();
        nemsEventProcessorUnhandledQueue.deleteAllMessages();
        notReallySuspensionsMessageQueue.deleteAllMessages();
    }

    @Test
    public void shouldMoveSuspensionMessageFromNemsToMofUpdatedQueue() throws Exception {
        String suspendedPatientNhsNumber = PATIENT_WHICH_HAS_NO_CURRENT_GP_NHS_NUMBER;

        PdsAdaptorClient pdsAdaptorClient = new PdsAdaptorClient(suspendedPatientNhsNumber);

        PdsAdaptorResponse pdsAdaptorResponse = pdsAdaptorClient.getSuspendedPatientStatus();

        pdsAdaptorClient.updateManagingOrganisation(PdsAdaptorTest.generateRandomOdsCode(), pdsAdaptorResponse.getRecordETag());

        NemsEventMessage nemsSuspension = helper.createNemsEventFromTemplate("change-of-gp-suspension.xml", suspendedPatientNhsNumber);

        String postedMessageId = meshMailbox.postMessage(nemsSuspension);

        then(() -> assertFalse(meshMailbox.hasMessageId(postedMessageId)));
        final List <Message> messageFromForwarder = meshForwarderQueue.readMessages();

        then(() -> assertThat(meshForwarderQueue.containsMessage(messageFromForwarder, nemsSuspension.body())));
        then(() -> assertFalse(meshMailbox.hasMessageId(postedMessageId)));

        then(() -> {
            final List<Message> suspensionMessages = suspensionsMessageQueue.readMessages();
            assertTrue(suspensionsMessageQueue.containsMessage(suspensionMessages, suspendedPatientNhsNumber));
        });

        then(() -> {
            final List<Message> mofUpdatedMessages = mofUpdatedMessageQueue.readMessages();
            assertTrue(mofUpdatedMessageQueue.containsMessage(mofUpdatedMessages, suspendedPatientNhsNumber));
        });
    }

    @Test
    public void shouldMoveSuspensionMessageWherePatientIsNoLongerSuspendedToNotSuspendedQueue() throws Exception {
        String currentlyRegisteredPatientNhsNumber = PATIENT_WHICH_HAS_CURRENT_GP_NHS_NUMBER;

        NemsEventMessage nemsSuspension = helper.createNemsEventFromTemplate("change-of-gp-suspension.xml", currentlyRegisteredPatientNhsNumber);

        String postedMessageId = meshMailbox.postMessage(nemsSuspension);

        waitForMeshMailboxRemovalOf(postedMessageId);

        final List <Message> forwarderQueueMsg = meshForwarderQueue.readMessages();



        then(() -> assertThat(meshForwarderQueue.containsMessage(forwarderQueueMsg, nemsSuspension.body())));

        then(() -> {
            List<Message> suspensionQueueMessages = suspensionsMessageQueue.readMessages();
            assertTrue(suspensionsMessageQueue.containsMessage(suspensionQueueMessages, currentlyRegisteredPatientNhsNumber));
        });
        then(() -> {
            List<Message> suspensionQueueMessage = notReallySuspensionsMessageQueue.readMessages();
            assertTrue(notReallySuspensionsMessageQueue.containsMessage(suspensionQueueMessage, currentlyRegisteredPatientNhsNumber));
        });
    }

    @Test
    public void shouldMoveNonSuspensionMessageFromNemsToUnhandledQueue() throws Exception {
        String nhsNumber = helper.randomNhsNumber();
        NemsEventMessage nemsNonSuspension = helper.createNemsEventFromTemplate("change-of-gp-non-suspension.xml", nhsNumber);

        String postedMessageId = meshMailbox.postMessage(nemsNonSuspension);
        log("Posted msg id is "+postedMessageId);

        log("Waiting for Forwarder to poll Mailbox for message");
        then(() -> assertFalse(meshMailbox.hasMessageId(postedMessageId)));

        final List <Message> forwarderQueueMsg = meshForwarderQueue.readMessages();

        then(() -> assertThat(meshForwarderQueue.containsMessage(forwarderQueueMsg, nemsNonSuspension.body())));

        then(() -> {
            final List<Message> nonSuspensionUnhandledMessages = nemsEventProcessorUnhandledQueue.readMessages();
            assertTrue(nemsEventProcessorUnhandledQueue.containsMessage(nonSuspensionUnhandledMessages, nemsNonSuspension.body()));
        });
    }

    private void waitForMeshMailboxRemovalOf(String postedMessageId) {
        log("Waiting for forwarder to remove message from mailbox");
        then(() -> assertFalse(meshMailbox.hasMessageId(postedMessageId)));
    }


    // This looks like it has been commented out because it was flaky - has left a lot of dead
    // code around, seems to be coverage over DLQ error cases for nems event processor
//    @Test
//    public void shouldSendUnprocessableMessagesToDlQ() throws Exception {
//        Map<String, NemsEventMessage> dlqMessages = helper.getDLQNemsEventMessages();
//        for (Map.Entry<String,NemsEventMessage> message :dlqMessages.entrySet()) {
//            log("Message to be posted is "+ message.getKey());
//            String postedMessageId = meshMailbox.postMessage(message.getValue());
//            then(() -> assertFalse(meshMailbox.hasMessageId(postedMessageId)));
//            assertMessageOnTheQueue(message.getValue(), nemsEventProcessorDeadLetterQueue);
//        }
//    }

    private void assertMessageOnTheQueue(NemsEventMessage message, NemsEventMessageQueue queue) {
        final List<Message> dlqMessages = queue.readMessages();
        then(() -> assertTrue(queue.containsMessage(dlqMessages, message.body())));
    }

    private void then(ThrowingRunnable assertion) {
        await().atMost(60, TimeUnit.SECONDS).with().pollInterval(2, TimeUnit.SECONDS).untilAsserted(assertion);
    }

    public void log(String message) {
        System.out.println(message);
    }
}
