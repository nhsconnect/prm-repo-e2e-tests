package uk.nhs.prm.deduction.e2e.performance;

import java.util.Date;
import java.util.Hashtable;

public class RecordingNemsPatientEventTestListener implements NemsPatientEventTestListener {
    public int testItemCount() {
        return nemsMessageIdToNhsNumberPairs.size();
    }

    final Hashtable nemsMessageIdToNhsNumberPairs = new Hashtable<>();

    @Override
    public void onStartingTestItem(String nemsMessageId, String nhsNumber) {
        nemsMessageIdToNhsNumberPairs.put(nemsMessageId, nhsNumber);
    }

    @Override
    public void onStartedTestItem(String nemsMessageId, String nhsNumber) {
        System.out.println("Started test on " + new Date() + " " + nemsMessageId + " " + nhsNumber);
    }
}
