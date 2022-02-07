package uk.nhs.prm.deduction.e2e.performance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import uk.nhs.prm.deduction.e2e.queue.SqsMessage;

import java.io.PrintStream;
import java.util.*;

import static java.util.stream.Collectors.toList;

public class RecordingNemsPatientEventTestListener implements NemsPatientEventTestListener, NemsTestRecording {
    private final Map<String, NemsTestEvent> nemsEventsById = new HashMap<>();
    private int knownEventCount = 0;
    private int unknownEventCount = 0;

    @Override
    public void onStartingTestItem(NemsTestEvent testEvent) {
        nemsEventsById.put(testEvent.nemsMessageId(), testEvent);
    }

    @Override
    public void onStartedTestItem(NemsTestEvent testEvent) {
        System.out.println("Started test on " + new Date() + " " + testEvent.nemsMessageId() + " " + testEvent.nhsNumber());
    }

    @Override
    public int testItemCount() {
        return nemsEventsById.size();
    }

    public boolean finishMatchingMessage(SqsMessage sqsMessage)  {
        String nemsMessageIdFromBody = extractNemsMessageIdFromBody(sqsMessage);
        if (nemsEventsById.containsKey(nemsMessageIdFromBody)) {
             if (nemsEventsById.get(nemsMessageIdFromBody).finished(sqsMessage)) {
                 knownEventCount++;
             };
            return true;
        } else {
            System.out.println("Nems message ID " + nemsMessageIdFromBody + " not sent by performance test");
            unknownEventCount++;
            return false;
        }
    }

    private String extractNemsMessageIdFromBody(SqsMessage sqsMessage) {
        JsonNode parent = null;
        try {
            parent = new ObjectMapper().readTree(sqsMessage.body());
        } catch (JsonProcessingException e) {
            return "failed to grab nemsMessageId";
        }
        return parent.get("nemsMessageId").asText();
    }

    @Override
    public void summariseTo(PrintStream out) {
        out.println("Total messages received: " + (knownEventCount + unknownEventCount));
        out.println("Total messages received from messages sent in test: " + knownEventCount);
        out.println("Total messages received from messasges received outside of test: " + unknownEventCount);
    }

    @Override
    public boolean hasUnfinishedEvents() {
        return knownEventCount < testItemCount();
    }

    @Override
    public List<NemsTestEvent> testEvents() {
        return nemsEventsById.values().stream().sorted().collect(toList());
    }
}
