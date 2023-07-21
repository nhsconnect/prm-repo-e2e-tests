package uk.nhs.prm.e2etests.performance;

import java.io.PrintStream;
import java.time.LocalDateTime;
import java.util.List;

public interface NemsTestRecording {
    void summariseTo(PrintStream out);

    boolean hasUnfinishedEvents();

    List<NemsTestEvent> testEvents();
    List<NemsTestEvent> startOrderedEvents();
    List<NemsTestEvent> finishOrderedEvents();

    default int testEventCount() {
        return testEvents().size();
    }

    default LocalDateTime runStartTime() {
        return startOrderedEvents().get(0).startedAt();
    }
}
