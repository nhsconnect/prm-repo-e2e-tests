package uk.nhs.prm.deduction.e2e.performance;

import uk.nhs.prm.deduction.e2e.nems.NemsEventMessage;
import uk.nhs.prm.deduction.e2e.nhs.NhsIdentityGenerator;
import uk.nhs.prm.deduction.e2e.performance.load.LoadPhase;
import uk.nhs.prm.deduction.e2e.performance.load.Phased;
import uk.nhs.prm.deduction.e2e.queue.SqsMessage;
import uk.nhs.prm.deduction.e2e.utility.NemsEventFactory;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static uk.nhs.prm.deduction.e2e.nhs.NhsIdentityGenerator.generateRandomOdsCode;

public class NemsTestEvent implements Comparable, Phased {
    private final String nemsMessageId;
    private final String nhsNumber;
    private final boolean suspension;

    private LoadPhase phase;
    private LocalDateTime started;
    private boolean isFinished = false;
    private long processingTimeMs;

    private List<String> warnings = new ArrayList<>();
    private LocalDateTime finishedAt;

    private NemsTestEvent(String nemsMessageId, String nhsNumber, boolean suspension) {
        this.nemsMessageId = nemsMessageId;
        this.nhsNumber = nhsNumber;
        this.suspension = suspension;
    }

    public static NemsTestEvent suspensionEvent(String nhsNumber, String nemsMessageId) {
        return new NemsTestEvent(nemsMessageId, nhsNumber, true);
    }

    public static NemsTestEvent nonSuspensionEvent(String nhsNumber, String nemsMessageId) {
        return new NemsTestEvent(nemsMessageId, nhsNumber, false);
    }

    public String nemsMessageId() {
        return nemsMessageId;
    }

    public String nhsNumber() {
        return nhsNumber;
    }

    @Override
    public void setPhase(LoadPhase phase) {
        this.phase = phase;
    }

    @Override
    public LoadPhase phase() {
        return phase;
    }

    public LocalDateTime startedAt() {
        return started;
    }

    public boolean isFinished() {
        return isFinished;
    }

    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }

    public void started() {
        this.started = LocalDateTime.now();
    }

    public boolean finished(SqsMessage successMessage) {
        boolean firstTimeFinisher = false;
        if (isFinished) {
            String warning = "Warning: Duplicate finisher! finished() but already isFinished";
            System.out.println(warning);
            warnings.add(warning);
        }
        else {
            firstTimeFinisher = true;
            isFinished = true;
            finishedAt = successMessage.queuedAt();
            processingTimeMs = startedAt().until(finishedAt, ChronoUnit.MILLIS);
        }

        System.out.println(this);

        return firstTimeFinisher;
    }

    public LocalDateTime finishedAt() {
        return finishedAt;
    }

    public long duration() {
        return processingTimeMs / 1000;
    }

    @Override
    public String toString() {
        return "NemsTestEvent{" +
                "nemsMessageId='" + nemsMessageId + '\'' +
                ", nhsNumber='" + nhsNumber + '\'' +
                ", suspension=" + suspension +
                ", phase=" + phase +
                ", started=" + started +
                ", isFinished=" + isFinished +
                ", processingTimeMs=" + processingTimeMs +
                ", warnings=" + warnings +
                ", finishedAt=" + finishedAt +
                '}';
    }

    @Override
    public int compareTo(Object o) {
        if (o.getClass().equals(NemsTestEvent.class)) {
            var otherEvent = (NemsTestEvent) o;
            return startedAt().compareTo(otherEvent.startedAt());
        }
        return 0;
    }

    public boolean isSuspension() {
        return suspension;
    }

    public NemsEventMessage createMessage() {
        var previousGP = generateRandomOdsCode();
        NemsEventMessage nemsSuspension;
        if (isSuspension()) {
            nemsSuspension = NemsEventFactory.createNemsEventFromTemplate("change-of-gp-suspension.xml",
                    nhsNumber(),
                    nemsMessageId(),
                    previousGP);
        }
        else {
            nemsSuspension = NemsEventFactory.createNemsEventFromTemplate("change-of-gp-non-suspension.xml",
                    nhsNumber(),
                    nemsMessageId());
        }
        return nemsSuspension;
    }
}
