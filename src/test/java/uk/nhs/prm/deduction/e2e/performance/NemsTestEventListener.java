package uk.nhs.prm.deduction.e2e.performance;

public interface NemsTestEventListener {
    void onStartingTestItem(NemsTestEvent testEvent);

    void onStartedTestItem(NemsTestEvent testEvent);
}
