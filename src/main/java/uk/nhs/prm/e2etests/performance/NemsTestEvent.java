package uk.nhs.prm.e2etests.performance;

import uk.nhs.prm.e2etests.performance.load.LoadPhase;
import uk.nhs.prm.e2etests.performance.load.Phased;
import uk.nhs.prm.e2etests.model.SqsMessage;
import uk.nhs.prm.e2etests.model.nems.NemsEventMessage;
import uk.nhs.prm.e2etests.utility.NemsEventFactory;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static uk.nhs.prm.e2etests.utility.NhsIdentityGenerator.randomOdsCode;

public class NemsTestEvent implements Phased {
    private final String nemsMessageId;
    private final String nhsNumber;
    private final boolean suspension;

    private String meshMessageId;
    private LoadPhase phase;
    private LocalDateTime startedAt;
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
        return startedAt;
    }

    public boolean isStarted() {
        return startedAt != null;
    }

    public boolean isFinished() {
        return isFinished;
    }

    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }

    public void started(String meshMessageId) {
        if (meshMessageId == null) {
            addWarning("No mesh message id received. Message may have not arrived in mesh mailbox.");
        }
        this.startedAt = LocalDateTime.now();
        this.meshMessageId = meshMessageId;
    }

    public boolean finished(SqsMessage successMessage) {
        boolean firstTimeFinisher = false;
        if (isFinished) {
            addWarning("Warning: Duplicate finisher! finished() but already isFinished");
        }
        else {
            firstTimeFinisher = true;
            isFinished = true;
            finishedAt = successMessage.getQueuedAt();
            processingTimeMs = startedAt().until(finishedAt, ChronoUnit.MILLIS);
        }
        return firstTimeFinisher;
    }

    private void addWarning(String warning) {
        System.out.println(warning);
        warnings.add(warning);
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
                ", meshMessageId='" + meshMessageId + '\'' +
                ", suspension=" + suspension +
                ", phase=" + phase +
                ", started=" + startedAt +
                ", isFinished=" + isFinished +
                ", processingTimeMs=" + processingTimeMs +
                ", warnings=" + warnings +
                ", finishedAt=" + finishedAt +
                '}';
    }

    public boolean isSuspension() {
        return suspension;
    }

    public NemsEventMessage createMessage() {
        String previousGP = randomOdsCode();
        NemsEventMessage nemsSuspension;
        String timestamp = ZonedDateTime.now(ZoneOffset.ofHours(0)).toString();

        if (isSuspension()) {
            nemsSuspension = NemsEventFactory.createNemsEventFromTemplate("change-of-gp-suspension.xml",
                    nhsNumber(),
                    nemsMessageId(),
                    previousGP,
                    timestamp);
        }
        else {
            nemsSuspension = NemsEventFactory.createNemsEventFromTemplate("change-of-gp-non-suspension.xml",
                    nhsNumber(),
                    nemsMessageId(),
                    timestamp);
        }
        return nemsSuspension;
    }


    public static Comparator<NemsTestEvent> finishedTimeOrder() {
        return (o1, o2) -> {
            if (o1.isFinished() && o2.isFinished()) {
                return o1.finishedAt().compareTo(o2.finishedAt());
            }
            return 0;
        };
    }

    public static Comparator<NemsTestEvent> startedTimeOrder() {
        return (o1, o2) -> {
            if (o1.isStarted() && o2.isStarted()) {
                return o1.startedAt().compareTo(o2.startedAt());
            }
            return 0;
        };
    }
}
