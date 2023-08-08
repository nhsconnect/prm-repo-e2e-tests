package uk.nhs.prm.e2etests.performance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import uk.nhs.prm.e2etests.model.SqsMessage;

import java.io.PrintStream;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

@Log4j2
public class PerformanceTestRecorder implements NemsTestEventListener, NemsTestRecording {
    private final Map<String, NemsTestEvent> nemsEventsById = new HashMap<>();
    private int knownEventCount = 0;
    private int unknownEventCount = 0;

    @Override
    public void onStartingTestItem(NemsTestEvent testEvent) {
        nemsEventsById.put(testEvent.getNemsMessageId(), testEvent);
        if (nemsEventsById.size() % 20 == 0) {
            log.info("Recorded {} test events at {}.", nemsEventsById.size(), new Date());
        }
    }

    @Override
    public void onStartedTestItem(NemsTestEvent testEvent) {
        log.info(testEvent.isSuspensionEvent() ? "Suspension Event" : "Non-Suspension Event");
    }

    public boolean finishMatchingMessage(SqsMessage sqsMessage)  {
        String nemsMessageIdFromBody = extractNemsMessageIdFromBody(sqsMessage);

        if (nemsEventsById.containsKey(nemsMessageIdFromBody)) {
            if (nemsEventsById.get(nemsMessageIdFromBody).finish(sqsMessage)) knownEventCount++;
            log.info("Known event found.");
            return true;
        } else {
            log.info("Unknown event found.");
            unknownEventCount++;
            return false;
        }
    }

    private String extractNemsMessageIdFromBody(SqsMessage sqsMessage) {
        JsonNode parent;
        try {
            parent = new ObjectMapper().readTree(sqsMessage.getBody());
        } catch (JsonProcessingException e) {
            return "failed to grab nemsMessageId";
        }
        return parent.get("nemsMessageId").asText();
    }

    @Override
    public void summariseTo(PrintStream out) {
        log.info("""
                Total messages received: {}
                Total messages received from messages sent in test: {}
                Total messages received from messages received outside of test: {}""",
                (knownEventCount + unknownEventCount), knownEventCount, unknownEventCount);

        testEvents().forEach(event -> {
            if (event.hasWarnings()) log.warn("Event had warnings: {}.", event);
            else if (!event.isFinished()) log.info("Event was never finished: {}.", event);
        });
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
