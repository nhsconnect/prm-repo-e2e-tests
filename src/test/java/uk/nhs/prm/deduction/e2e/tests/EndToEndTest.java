package uk.nhs.prm.deduction.e2e.tests;

import org.awaitility.core.ThrowingRunnable;
import org.junit.jupiter.api.Test;
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
import uk.nhs.prm.deduction.e2e.suspensions.NemsEventProcessorSuspensionsMessageQueue;
import uk.nhs.prm.deduction.e2e.suspensions.SuspensionServiceNotReallySuspensionsMessageQueue;
import uk.nhs.prm.deduction.e2e.utility.Helper;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;


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
        Helper.class
})
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
    private NemsEventProcessorDeadLetterQueue nemsEventProcessorDeadLetterQueue;
    @Autowired
    private MeshMailbox meshMailbox;
    @Autowired
    private Helper helper;
    @Test
    public void shouldMoveSuspensionMessageFromNemsToSuspensionsObservabilityQueue() throws Exception {
        String nhsNumber = helper.randomNhsNumber();

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

    @Test
    public void shouldSendUnprocessableMessagesToDlQ() throws Exception {
        Map<String, NemsEventMessage> dlqMessages = helper.getDLQNemsEventMessages();
        for (Map.Entry<String,NemsEventMessage> message :dlqMessages.entrySet()) {
            log("Message to be posted is "+ message.getKey());
            String postedMessageId = meshMailbox.postMessage(message.getValue());
            assertMessageOnTheQueue(message.getValue(), postedMessageId, nemsEventProcessorDeadLetterQueue);
        }
    }

    private void assertMessageOnTheQueue(NemsEventMessage message, String postedMessageId, NemsEventMessageQueue queue) {
        then(() -> assertFalse(meshMailbox.hasMessageId(postedMessageId)));
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
