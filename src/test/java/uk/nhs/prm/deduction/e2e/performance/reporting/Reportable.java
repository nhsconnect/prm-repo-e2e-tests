package uk.nhs.prm.deduction.e2e.performance.reporting;

import java.io.PrintStream;

public interface Reportable {
    void summariseTo(PrintStream out);
}
