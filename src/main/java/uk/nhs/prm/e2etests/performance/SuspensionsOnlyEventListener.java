package uk.nhs.prm.e2etests.performance;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class SuspensionsOnlyEventListener implements NemsTestEventListener {
    private final NemsTestEventListener recorder;

    public SuspensionsOnlyEventListener(NemsTestEventListener recorder) {
        this.recorder = recorder;
    }

    @Override
    public void onStartingTestItem(NemsTestEvent testEvent) {
        if (testEvent.isSuspensionEvent()) {
            recorder.onStartingTestItem(testEvent);
        }
    }

    @Override
    public void onStartedTestItem(NemsTestEvent testEvent) {
        if (testEvent.isSuspensionEvent()) {
            recorder.onStartedTestItem(testEvent);
        } else {
            log.info("Unknown Event Found");
        }
    }
}
