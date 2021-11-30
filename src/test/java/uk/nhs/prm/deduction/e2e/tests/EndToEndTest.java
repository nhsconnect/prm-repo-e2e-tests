package uk.nhs.prm.deduction.e2e.tests;

import org.awaitility.core.ThrowingRunnable;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.nhs.prm.deduction.e2e.TestConfiguration;
import uk.nhs.prm.deduction.e2e.auth.AuthTokenGenerator;
import uk.nhs.prm.deduction.e2e.mesh.MeshClient;
import uk.nhs.prm.deduction.e2e.mesh.MeshMailbox;
import uk.nhs.prm.deduction.e2e.nems.NemsEventMessage;
import uk.nhs.prm.deduction.e2e.nems.NemsEventMessageQueue;
import uk.nhs.prm.deduction.e2e.queue.SqsQueue;
import uk.nhs.prm.deduction.e2e.suspensions.SuspensionMessage;
import uk.nhs.prm.deduction.e2e.suspensions.SuspensionMessageQueue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest(classes = {
        EndToEndTest.class,
        NemsEventMessageQueue.class,
        MeshMailbox.class,
        SqsQueue.class,
        MeshClient.class,
        TestConfiguration.class,
        AuthTokenGenerator.class,
        SuspensionMessageQueue.class
})
public class EndToEndTest {

    @Autowired
    private TestConfiguration configuration;
    @Autowired
    private NemsEventMessageQueue meshForwarderQueue;
    @Autowired
    private SuspensionMessageQueue suspensionMessageQueue;
    @Autowired
    private MeshMailbox meshMailbox;

 //Todo write a start method that starts with cleaning up the queue

    @Test
    public void shouldMoveSuspensionMessageFromNemsToSuspensionsObservabilityQueue() throws Exception {
        String nhsNumber = randomNhsNumber();

        NemsEventMessage nemsSuspensionMessage = createNemsEventFromTemplate("change-of-gp-suspension.xml", nhsNumber);

        String postedMessageId = meshMailbox.postMessage(nemsSuspensionMessage);

        then(() -> assertThat(readForwardedMeshEvent().body()).contains(nemsSuspensionMessage.body()));

        then(() -> assertFalse(meshMailbox.hasMessageId(postedMessageId)));

        then(() -> assertEquals(readSuspensionMessage().nhsNumber(), nhsNumber));

        then(() -> assertEquals(readNotReallySuspendedMessage().nhsNumber(), nhsNumber));
//Todo delete messages on the queue once read
    }

    @Test
    public void shouldMoveNonSuspensionMessageFromNemsToUnhandledQueue() throws Exception {
        NemsEventMessage nemsNonSuspensionMessage = createNemsEventFromTemplate("change-of-gp-non-suspension.xml", randomNhsNumber());

        String postedMessageId = meshMailbox.postMessage(nemsNonSuspensionMessage);

        then(() -> assertThat(readForwardedMeshEvent().body()).contains(nemsNonSuspensionMessage.body()));
        then(() -> assertFalse(meshMailbox.hasMessageId(postedMessageId)));

        then(() -> assertEquals(readUnhandledNemsEvent().body(), nemsNonSuspensionMessage.body()));
//Todo delete messages on the queue once read
    }

    private String randomNhsNumber() {
        return "991200" + (System.currentTimeMillis() % 10000);
    }

    private NemsEventMessage readForwardedMeshEvent() {
        return meshForwarderQueue.readEventMessage(configuration.meshForwarderObservabilityQueueUri());
    }

    private NemsEventMessage readUnhandledNemsEvent() {
        return meshForwarderQueue.readEventMessage(configuration.nemsEventProcesorUnhandledQueueUri());
    }

    private SuspensionMessage readSuspensionMessage() throws JSONException {
        return suspensionMessageQueue.readEventMessage(configuration.suspensionsObservabilityQueueUri());
    }

    private SuspensionMessage readNotReallySuspendedMessage() throws JSONException {
        return suspensionMessageQueue.readEventMessage(configuration.notReallySuspendedObservabilityQueueUri());
    }

    private void then(ThrowingRunnable assertion) {
        await().atMost(60, TimeUnit.SECONDS).with().pollInterval(5, TimeUnit.SECONDS).untilAsserted(assertion);
    }

    private NemsEventMessage createNemsEventFromTemplate(String nemsEventFilename, String nhsNumber) throws IOException {
        return new NemsEventMessage(readTestResourceFile(nemsEventFilename).replaceAll("__NHS_NUMBER__", nhsNumber));
    }

    public void log(String messageBody, String messageValue) {
        System.out.println(String.format(messageBody, messageValue));
    }

    private String readTestResourceFile(String nemsEvent) throws IOException {
        File file = new File(String.format("src/test/resources/%s", nemsEvent));
        BufferedReader br = new BufferedReader(new FileReader(file));
        String line;
        StringBuilder sb = new StringBuilder();

        while((line=br.readLine())!= null){
            sb.append(line.trim());
        }
        return sb.toString();
    }
}
