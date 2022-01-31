package uk.nhs.prm.deduction.e2e.performance;

public interface NemsPatientEventTestListener {
    void onStartingTestItem(String nemsMessageId, String nhsNumber);

    void onStartedTestItem(String nemsMessageId, String nhsNumber);
}
