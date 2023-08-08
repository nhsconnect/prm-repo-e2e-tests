package uk.nhs.prm.e2etests.performance;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import uk.nhs.prm.e2etests.model.SqsMessage;
import uk.nhs.prm.e2etests.performance.load.LoadPhase;
import uk.nhs.prm.e2etests.performance.load.Phased;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Log4j2
@Getter
public class NemsTestEvent implements Phased {
    private final String nemsMessageId;
    private final String nhsNumber;
    private final boolean isSuspensionEvent;
    private final List<String> warnings;
    private String meshMessageId;
    private LoadPhase phase;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private long processingTimeSeconds;

    private NemsTestEvent(String nemsMessageId, String nhsNumber, boolean isSuspensionEvent) {
        this.nemsMessageId = nemsMessageId;
        this.nhsNumber = nhsNumber;
        this.isSuspensionEvent = isSuspensionEvent;
        this.warnings = new ArrayList<>();
    }

    public static NemsTestEvent suspensionEvent(String nhsNumber, String nemsMessageId) {
        return new NemsTestEvent(nemsMessageId, nhsNumber, true);
    }

    public static NemsTestEvent nonSuspensionEvent(String nhsNumber, String nemsMessageId) {
        return new NemsTestEvent(nemsMessageId, nhsNumber, false);
    }

    public void start(String meshMessageId) {
        if (meshMessageId == null) {
            addWarning("No mesh message id received. Message may have not arrived in mesh mailbox.");
        }
        this.startedAt = LocalDateTime.now();
        this.meshMessageId = meshMessageId;
    }

    public boolean finish(SqsMessage successMessage) {
        boolean hasBeenCalled = false;
        if (isFinished()) {
            addWarning("Warning: Duplicate finisher! finished() but already isFinished");
        }
        else {
            hasBeenCalled = true;
            finishedAt = successMessage.getQueuedAt();
            processingTimeSeconds = (getStartedAt().until(finishedAt, ChronoUnit.MILLIS) / 1000);
        }
        return hasBeenCalled;
    }

    private void addWarning(String warning) {
        log.warn(warning);
        warnings.add(warning);
    }

    public static Comparator<NemsTestEvent> startedTimeOrder() {
        return (o1, o2) -> {
            if (o1.isStarted() && o2.isStarted()) {
                return o1.getStartedAt().compareTo(o2.getStartedAt());
            }
            return 0;
        };
    }

    public static Comparator<NemsTestEvent> finishedTimeOrder() {
        return (o1, o2) -> {
            if (o1.isFinished() && o2.isFinished()) {
                return o1.getFinishedAt().compareTo(o2.getFinishedAt());
            }
            return 0;
        };
    }

    @Override
    public String toString() {
        return String.format("NemsTestEvent{nemsMessageId=%s, nhsNumber=%s, meshMessageId=%s, suspension=%s, " +
                        "phase=%s, started=%s, isFinished=%s, processingTimeMs=%s, warnings=%s, finishedAt=%s}",
                nemsMessageId, nhsNumber, meshMessageId, isSuspensionEvent, phase,
                startedAt, isFinished(), processingTimeSeconds, warnings, finishedAt);
    }

    @Override
    public LoadPhase phase() {
        return phase;
    }

    @Override
    public void setPhase(LoadPhase phase) {
        this.phase = phase;
    }

    public boolean isStarted() {
        return startedAt != null;
    }

    public boolean isFinished() {
        return finishedAt != null;
    }

    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }
}
