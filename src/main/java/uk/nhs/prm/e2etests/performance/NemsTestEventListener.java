package uk.nhs.prm.e2etests.performance;

public interface NemsTestEventListener {
    void onStartingTestItem(NemsTestEvent testEvent);

    void onStartedTestItem(NemsTestEvent testEvent);
}
