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
import uk.nhs.prm.deduction.e2e.queue.SqsQueue;
import uk.nhs.prm.deduction.e2e.suspensions.MofUpdatedMessageQueue;
import uk.nhs.prm.deduction.e2e.suspensions.NemsEventProcessorSuspensionsMessageQueue;
import uk.nhs.prm.deduction.e2e.suspensions.SuspensionServiceNotReallySuspensionsMessageQueue;
import uk.nhs.prm.deduction.e2e.utility.Helper;

import java.util.List;
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
        MofUpdatedMessageQueue.class
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class EndToEndTest {

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
//        String nhsNumber = randomNhsNumber();
        //Suspended patient nhs number
        String nhsNumber = "9693797515";
        NemsEventMessage nemsSuspension = helper.createNemsEventFromTemplate("change-of-gp-suspension.xml", nhsNumber);

        String postedMessageId = meshMailbox.postMessage(nemsSuspension);

        final List <Message> forwarderQueueMsg = meshForwarderQueue.readMessages();

        then(() -> assertThat(meshForwarderQueue.containsMessage(forwarderQueueMsg, nemsSuspension.body())));

        then(() -> assertFalse(meshMailbox.hasMessageId(postedMessageId)));

        final List<Message> suspensionMessages = suspensionsMessageQueue.readMessages();
        then(() -> assertTrue(suspensionsMessageQueue.containsMessage(suspensionMessages, nhsNumber)));

        final List<Message> mofUpdatedMessages = mofUpdatedMessageQueue.readMessages();
        then(() -> assertTrue(mofUpdatedMessageQueue.containsMessage(mofUpdatedMessages, nhsNumber)));
    }

    @Test
    public void shouldMoveSuspensionMessageFromNemsToSuspensionsObservabilityQueue() throws Exception {
//        String nhsNumber = randomNhsNumber();
        //Not-Suspended patient nhs number
        String nhsNumber = "9692294994";

        NemsEventMessage nemsSuspension = helper.createNemsEventFromTemplate("change-of-gp-suspension.xml", nhsNumber);

        String postedMessageId = meshMailbox.postMessage(nemsSuspension);
        log("Posted msg id is "+postedMessageId);

        log("Waiting for Forwarder to poll Mailbox for message");
        then(() -> assertFalse(meshMailbox.hasMessageId(postedMessageId)));

        final List <Message> forwarderQueueMsg = meshForwarderQueue.readMessages();

        log("Checking if message is present on the queue");

        then(() -> assertThat(meshForwarderQueue.containsMessage(forwarderQueueMsg, nemsSuspension.body())));

        final List<Message> NemsSuspensionQueueMessages = suspensionsMessageQueue.readMessages();

            then(() -> assertTrue(suspensionsMessageQueue.containsMessage(NemsSuspensionQueueMessages, nhsNumber)));
            final List<Message> suspensionQueueMessage = notReallySuspensionsMessageQueue.readMessages();
            then(() -> assertTrue(notReallySuspensionsMessageQueue.containsMessage(suspensionQueueMessage, nhsNumber)));

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

        log("Checking if message is present on the queue");

        then(() -> assertThat(meshForwarderQueue.containsMessage(forwarderQueueMsg, nemsNonSuspension.body())));

        final List<Message> nonSuspensionUnhandledMessages = nemsEventProcessorUnhandledQueue.readMessages();
        log("Checking if message is present on the queue");
        then(() -> assertTrue(nemsEventProcessorUnhandledQueue.containsMessage(nonSuspensionUnhandledMessages,nemsNonSuspension.body())));
    }

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
