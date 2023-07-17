package uk.nhs.prm.e2etests.performance;

public class DoNothingTestEventListener implements NemsTestEventListener {
    @Override
    public void onStartingTestItem(NemsTestEvent testEvent) {
        // nop
    }

    @Override
    public void onStartedTestItem(NemsTestEvent testEvent) {
        // nop
    }
}
