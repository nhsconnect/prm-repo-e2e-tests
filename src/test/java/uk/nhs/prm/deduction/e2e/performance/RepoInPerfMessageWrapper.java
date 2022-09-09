package uk.nhs.prm.deduction.e2e.performance;

import uk.nhs.prm.deduction.e2e.models.RepoIncomingMessage;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class RepoInPerfMessageWrapper {

    private final LocalDateTime startedAt;
    private final RepoIncomingMessage message;
    private LocalDateTime finishedAt;

    public RepoInPerfMessageWrapper(RepoIncomingMessage message) {
        this.message = message;
        this.startedAt = LocalTime.now().atDate(LocalDate.now());
    }

    public RepoIncomingMessage getMessage() {
        return message;
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
}
