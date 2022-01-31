package uk.nhs.prm.deduction.e2e.performance;

public class DoNothingTestListener implements NemsPatientEventTestListener {
    @Override
    public void onStartingTestItem(String nemsMessageId, String nhsNumber) {
        // nop
    }

    @Override
    public void onStartedTestItem(String nemsMessageId, String nhsNumber) {
        // nop
    }
}
