package uk.nhs.prm.deduction.e2e.performance;

public interface NemsPatientEventTestListener {
    void onStartingTestItem(NemsTestEvent testEvent);

    void onStartedTestItem(NemsTestEvent testEvent);
}
