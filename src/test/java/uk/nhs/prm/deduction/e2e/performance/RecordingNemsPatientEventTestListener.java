package uk.nhs.prm.deduction.e2e.performance;

import java.util.Date;
import java.util.Hashtable;

public class RecordingNemsPatientEventTestListener implements NemsPatientEventTestListener {
    public int testItemCount() {
        return nemsMessageIdToNhsNumberPairs.size();
    }

    final Hashtable nemsMessageIdToNhsNumberPairs = new Hashtable<>();

    @Override
    public void onStartingTestItem(NemsTestEvent testEvent) {
        nemsMessageIdToNhsNumberPairs.put(testEvent.nemsMessageId(), testEvent);
    }

    @Override
    public void onStartedTestItem(NemsTestEvent testEvent) {
        System.out.println("Started test on " + new Date() + " " + testEvent.nemsMessageId() + " " + testEvent.nhsNumber());
    }
}
