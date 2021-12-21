package uk.nhs.prm.deduction.e2e.tests;

import org.awaitility.core.ThrowingRunnable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.nhs.prm.deduction.e2e.TestConfiguration;
import uk.nhs.prm.deduction.e2e.auth.AuthTokenGenerator;
import uk.nhs.prm.deduction.e2e.mesh.MeshClient;
import uk.nhs.prm.deduction.e2e.mesh.MeshMailbox;
import uk.nhs.prm.deduction.e2e.nems.MeshForwarderQueue;
import uk.nhs.prm.deduction.e2e.nems.NemsEventMessage;
import uk.nhs.prm.deduction.e2e.nems.NemsEventProcessorUnhandledQueue;
import uk.nhs.prm.deduction.e2e.queue.SqsQueue;
import uk.nhs.prm.deduction.e2e.suspensions.MofUpdatedMessageQueue;
import uk.nhs.prm.deduction.e2e.suspensions.NemsEventProcessorSuspensionsMessageQueue;
import uk.nhs.prm.deduction.e2e.suspensions.SuspensionServiceNotReallySuspensionsMessageQueue;

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
        MeshMailbox.class,
        SqsQueue.class,
        MeshClient.class,
        TestConfiguration.class,
        AuthTokenGenerator.class,
        MeshForwarderQueue.class,
        NemsEventProcessorUnhandledQueue.class,
        NemsEventProcessorSuspensionsMessageQueue.class,
        SuspensionServiceNotReallySuspensionsMessageQueue.class,
        MofUpdatedMessageQueue.class
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
    private MofUpdatedMessageQueue mofUpdatedMessageQueue;
    @Autowired
    private MeshMailbox meshMailbox;

    @Test
    public void shouldMoveSuspensionMessageFromNemsToMofUpdatedQueue() throws Exception {
//        String nhsNumber = randomNhsNumber();
        //Suspended patient nhs number
        String nhsNumber = "9693797515";
        NemsEventMessage nemsSuspension = createNemsEventFromTemplate("change-of-gp-suspension.xml", nhsNumber);

        String postedMessageId = meshMailbox.postMessage(nemsSuspension);

        then(() -> assertThat(meshForwarderQueue.readMessage().body()).contains(nemsSuspension.body()));

        then(() -> assertFalse(meshMailbox.hasMessageId(postedMessageId)));

        then(() -> assertEquals(suspensionsMessageQueue.readMessage().nhsNumber(), nhsNumber));

        then(() -> assertEquals(mofUpdatedMessageQueue.readMessage().nhsNumber(), nhsNumber));
    }

    @Test
    public void shouldMoveSuspensionMessageFromNemsToSuspensionsObservabilityQueue() throws Exception {
//        String nhsNumber = randomNhsNumber();
        //Not-Suspended patient nhs number
        String nhsNumber = "9692294994";
        NemsEventMessage nemsSuspension = createNemsEventFromTemplate("change-of-gp-suspension.xml", nhsNumber);

        String postedMessageId = meshMailbox.postMessage(nemsSuspension);

        then(() -> assertThat(meshForwarderQueue.readMessage().body()).contains(nemsSuspension.body()));

        then(() -> assertFalse(meshMailbox.hasMessageId(postedMessageId)));

        then(() -> assertEquals(suspensionsMessageQueue.readMessage().nhsNumber(), nhsNumber));

        then(() -> assertEquals(notReallySuspensionsMessageQueue.readMessage().nhsNumber(), nhsNumber));
    }

    @Test
    public void shouldMoveNonSuspensionMessageFromNemsToUnhandledQueue() throws Exception {
        NemsEventMessage nemsNonSuspension = createNemsEventFromTemplate("change-of-gp-non-suspension.xml", randomNhsNumber());

        String postedMessageId = meshMailbox.postMessage(nemsNonSuspension);

        then(() -> assertThat(meshForwarderQueue.readMessage().body()).contains(nemsNonSuspension.body()));
        then(() -> assertFalse(meshMailbox.hasMessageId(postedMessageId)));

        then(() -> assertEquals(nemsEventProcessorUnhandledQueue.readMessage().body(), nemsNonSuspension.body()));
    }

    private String randomNhsNumber() {
        return "99120" + (System.currentTimeMillis() % 100000);
    }

    private void then(ThrowingRunnable assertion) {
        await().atMost(60, TimeUnit.SECONDS).with().pollInterval(2, TimeUnit.SECONDS).untilAsserted(assertion);
    }

    private NemsEventMessage createNemsEventFromTemplate(String nemsEventFilename, String nhsNumber) throws IOException {
        return new NemsEventMessage(readTestResourceFile(nemsEventFilename).replaceAll("__NHS_NUMBER__", nhsNumber));
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
