package uk.nhs.prm.deduction.e2e.performance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import uk.nhs.prm.deduction.e2e.queue.SqsMessage;

import java.io.PrintStream;
import java.util.*;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public class PerformanceTestRecorder implements NemsTestEventListener, NemsTestRecording {
    private final Map<String, NemsTestEvent> nemsEventsById = new HashMap<>();
    private int knownEventCount = 0;
    private int unknownEventCount = 0;

    @Override
    public void onStartingTestItem(NemsTestEvent testEvent) {
        nemsEventsById.put(testEvent.nemsMessageId(), testEvent);
        if (nemsEventsById.size() % 20 == 0) {
            System.out.println();
            System.out.println("Recorded " + nemsEventsById.size() + " test events at " + new Date());
        }
    }

    @Override
    public void onStartedTestItem(NemsTestEvent testEvent) {
        System.out.print(testEvent.isSuspension() ? "S" : "n");
    }

    public boolean finishMatchingMessage(SqsMessage sqsMessage)  {
        String nemsMessageIdFromBody = extractNemsMessageIdFromBody(sqsMessage);
        if (nemsEventsById.containsKey(nemsMessageIdFromBody)) {
            if (nemsEventsById.get(nemsMessageIdFromBody).finished(sqsMessage)) {
                knownEventCount++;
            };
            System.out.print("M");
            return true;
        } else {
            System.out.print(".");
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
        for (var event : testEvents()) {
            if (event.hasWarnings()) {
                System.out.println("Event had warnings: " + event);
            }
            if (!event.isFinished()) {
                System.out.println("Event was never finished: " + event);
            }
        }
    }

    @Override
    public boolean hasUnfinishedEvents() {
        return knownEventCount < testEventCount();
    }

    @Override
    public List<NemsTestEvent> testEvents() {
        return eventStream().collect(toList());
    }

    @Override
    public List<NemsTestEvent> startOrderedEvents() {
        return eventStream()
                .filter(NemsTestEvent::isStarted)
                .sorted(NemsTestEvent.startedTimeOrder())
                .collect(toList());
    }

    @Override
    public List<NemsTestEvent> finishOrderedEvents() {
        return eventStream()
                .filter(NemsTestEvent::isFinished)
                .sorted(NemsTestEvent.finishedTimeOrder())
                .collect(toList());
    }

    private Stream<NemsTestEvent> eventStream() {
        return nemsEventsById.values().stream();
    }
}
