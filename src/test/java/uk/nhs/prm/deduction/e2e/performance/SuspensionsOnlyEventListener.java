package uk.nhs.prm.deduction.e2e.performance;

public class SuspensionsOnlyEventListener implements NemsTestEventListener {
    private NemsTestEventListener recorder;

    public SuspensionsOnlyEventListener(NemsTestEventListener recorder) {
        this.recorder = recorder;
    }

    @Override
    public void onStartingTestItem(NemsTestEvent testEvent) {
        if (testEvent.isSuspension()) {
            recorder.onStartingTestItem(testEvent);
        }
    }

    @Override
    public void onStartedTestItem(NemsTestEvent testEvent) {
        if (testEvent.isSuspension()) {
            recorder.onStartedTestItem(testEvent);
        }
    }
}
