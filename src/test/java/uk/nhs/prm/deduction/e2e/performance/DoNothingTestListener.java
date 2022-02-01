package uk.nhs.prm.deduction.e2e.performance;

public class DoNothingTestListener implements NemsPatientEventTestListener {
    @Override
    public void onStartingTestItem(NemsTestEvent testEvent) {
        // nop
    }

    @Override
    public void onStartedTestItem(NemsTestEvent testEvent) {
        // nop
    }
}
