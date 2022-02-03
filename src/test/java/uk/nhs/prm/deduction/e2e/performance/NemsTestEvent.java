package uk.nhs.prm.deduction.e2e.performance;

import uk.nhs.prm.deduction.e2e.queue.SqsMessage;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

import static java.time.LocalTime.now;

public class NemsTestEvent {
    private final String nemsMessageId;
    private final String nhsNumber;

    private final LocalTime createdAt;
    private LocalDateTime started;

    public NemsTestEvent(String nemsMessageId, String nhsNumber) {
        this.nemsMessageId = nemsMessageId;
        this.nhsNumber = nhsNumber;
        this.createdAt = now();
    }

    public String nemsMessageId() {
        return nemsMessageId;
    }

    public String nhsNumber() {
        return nhsNumber;
    }

    public LocalDateTime startedAt() {
        return started;
    }

    public void started() {
        this.started = LocalDateTime.now();
    }

    public void finished(SqsMessage successMessage) {
        var processingTimeMs = startedAt().until(successMessage.queuedAt(), ChronoUnit.MILLIS);

        System.out.println(String.format("NEMS suspension %s for %s was injected at %tT and arrived on output queue at %tT after %s ms",
                nemsMessageId(),
                nhsNumber(),
                startedAt(),
                successMessage.queuedAt(),
                processingTimeMs));
    }
}
