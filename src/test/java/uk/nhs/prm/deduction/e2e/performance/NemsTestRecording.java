package uk.nhs.prm.deduction.e2e.performance;

import java.io.PrintStream;
import java.util.List;

public interface NemsTestRecording {
    int testItemCount();

    void summariseTo(PrintStream out);

    boolean hasUnfinishedEvents();

    List<NemsTestEvent> testEvents();
}
