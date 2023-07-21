package uk.nhs.prm.e2etests.performance;

import uk.nhs.prm.e2etests.model.RepoIncomingMessage;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

public class RepoInPerfMessageWrapper {

    private LocalDateTime startedAt;
    private final RepoIncomingMessage message;
    private LocalDateTime finishedAt;

    public RepoInPerfMessageWrapper(RepoIncomingMessage message) {
        this.message = message;
    }

    public RepoIncomingMessage getMessage() {
        return message;
    }

    public void start() {
        if (null == startedAt) {
            startedAt = LocalTime.now().atDate(LocalDate.now());
        }
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void finish(LocalDateTime localDateTime) {
        if (null == finishedAt) {
            finishedAt = localDateTime;
        }
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    public long getProcessingTimeInSeconds() {
        if (null == startedAt) {
            return 0;
        }

        if (null == finishedAt) {
            return startedAt.until(LocalTime.now().atDate(LocalDate.now()), ChronoUnit.SECONDS);
        }

        return startedAt.until(finishedAt, ChronoUnit.SECONDS);
    }
}
