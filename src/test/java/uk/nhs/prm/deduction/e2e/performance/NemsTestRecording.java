package uk.nhs.prm.deduction.e2e.performance;

import java.io.PrintStream;
import java.time.LocalDateTime;
import java.util.List;

public interface NemsTestRecording {
    void summariseTo(PrintStream out);

    boolean hasUnfinishedEvents();

    List<NemsTestEvent> testEvents();

    default int testItemCount() {
        return testEvents().size();
    }

    default LocalDateTime runStartTime() {
        return testEvents().get(0).startedAt();
    }
}
