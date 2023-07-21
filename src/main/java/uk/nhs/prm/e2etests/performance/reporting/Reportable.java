package uk.nhs.prm.e2etests.performance.reporting;

import java.io.PrintStream;

public interface Reportable {
    void summariseTo(PrintStream out);
}
