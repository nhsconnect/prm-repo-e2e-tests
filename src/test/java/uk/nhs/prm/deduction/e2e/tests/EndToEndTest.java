package uk.nhs.prm.deduction.e2e.tests;

import org.json.JSONException;
import org.json.JSONObject;
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest(classes = {EndToEndTest.class,NemsEventMessageQueue.class,MeshMailbox.class, SqsQueue.class, MeshClient.class, TestConfiguration.class,AuthTokenGenerator.class})
public class EndToEndTest {

    @Autowired
    private TestConfiguration configuration ;
    @Autowired
    private NemsEventMessageQueue queue;
    @Autowired
    private MeshMailbox meshMailbox;

 //Todo write a start method that starts with cleaning up the queue

    @Test
    public void shouldMoveSuspensionMessageFromNemsToSuspensionsObservabilityQueue() throws Exception {
        NemsEventMessage nemsSuspensionMessage = someNemsEvent("change-of-gp-suspension.xml");

        String postedMessageId = meshMailbox.postMessage(nemsSuspensionMessage);

        validateThatMessageLandsCorrectlyOnMeshObsverabilityQueue(postedMessageId,nemsSuspensionMessage);

        validateThatMessageLandsCorrectlyOnSuspensionsObservabilityQueue(postedMessageId);

        validateThatMessageLandsCorrectlyOnNonSuspendedObservabilityQueue(postedMessageId);

//Todo delete messages on the queue once read
    }



    @Test
    public void shouldMoveNonSuspensionMessageFromNemsToUnhandledQueue() throws Exception {
        NemsEventMessage nemsNonSuspensionMessage = someNemsEvent("change-of-gp-non-suspension.xml");

        String postedMessageId = meshMailbox.postMessage(nemsNonSuspensionMessage);

        validateThatMessageLandsCorrectlyOnMeshObsverabilityQueue(postedMessageId,nemsNonSuspensionMessage);

        validateThatMessageLandsCorrectlyOnNemsEventUnhandledQueue(postedMessageId, nemsNonSuspensionMessage);

//Todo delete messages on the queue once read
    }

    private void validateThatMessageLandsCorrectlyOnNonSuspendedObservabilityQueue(String postedMessageId) {
        await().atMost(60, TimeUnit.SECONDS).with().pollInterval(10, TimeUnit.SECONDS).untilAsserted(() -> {
            Map<String, String> response= getNemsEventParserResponse(queue.readEventMessage(configuration.NonSuspendedObservabilityQueueUri()).body());
            assertEquals(response.get("nhsNumber"),"9912003888");
            assertFalse(meshMailbox.hasMessageId(postedMessageId));
        });
    }
    private void validateThatMessageLandsCorrectlyOnMeshObsverabilityQueue(String postedMessageId,NemsEventMessage nemsEventMessage) {
        await().atMost(60, TimeUnit.SECONDS).with().pollInterval(10, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(queue.readEventMessage(configuration.meshForwarderObservabilityQueueUri()).body()).contains(nemsEventMessage.body());
            assertFalse(meshMailbox.hasMessageId(postedMessageId));
        });
    }
    private void validateThatMessageLandsCorrectlyOnSuspensionsObservabilityQueue(String postedMessageId) {
        await().atMost(60, TimeUnit.SECONDS).with().pollInterval(10, TimeUnit.SECONDS).untilAsserted(() -> {
            Map<String, String> response= getNemsEventParserResponse(queue.readEventMessage(configuration.suspensionsObservabilityQueueUri()).body());
            assertEquals(response.get("nhsNumber"),"9912003888");
            assertFalse(meshMailbox.hasMessageId(postedMessageId));
        });
    }
    private void validateThatMessageLandsCorrectlyOnNemsEventUnhandledQueue(String postedMessageId, NemsEventMessage nemsEventMessage) {
        await().atMost(60, TimeUnit.SECONDS).with().pollInterval(10, TimeUnit.SECONDS).untilAsserted(() -> {
            String unhandledMessageBody= queue.readEventMessage(configuration.NemsEventProcesorUnhandledQueueUri()).body();
            assertEquals(unhandledMessageBody,nemsEventMessage.body());
            assertFalse(meshMailbox.hasMessageId(postedMessageId));
        });
    }


    private NemsEventMessage someNemsEvent(String nemsEvent) throws IOException {
        return new NemsEventMessage(readXmlFile(nemsEvent));
    }

    public void log(String messageBody, String messageValue) {
        System.out.println(String.format(messageBody, messageValue));
    }
    private String readXmlFile(String nemsEvent) throws IOException {
        File file = new File(String.format("src/test/resources/%s", nemsEvent));
        BufferedReader br = new BufferedReader(new FileReader(file));
        String line;
        StringBuilder sb = new StringBuilder();

        while((line=br.readLine())!= null){
            sb.append(line.trim());
        }
        return sb.toString();
    }

    private Map<String, String> getNemsEventParserResponse(String responseBody) throws JSONException {
        Map<String, String> response = new HashMap<>();
        JSONObject jsonObject = new JSONObject(responseBody);
        response.put("nhsNumber",jsonObject.get("nhsNumber").toString());
        response.put("eventType",jsonObject.get("eventType").toString());
        response.put("previousOdsCode",jsonObject.get("previousOdsCode").toString());
        response.put("lastUpdated",jsonObject.get("lastUpdated").toString());
        return response;

    }
}
