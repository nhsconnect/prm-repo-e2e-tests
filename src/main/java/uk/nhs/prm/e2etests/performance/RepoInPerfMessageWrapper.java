package uk.nhs.prm.e2etests.performance;

import lombok.Getter;
import uk.nhs.prm.e2etests.model.RepoIncomingMessage;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

@Getter
public class RepoInPerfMessageWrapper {

    private LocalDateTime startedAt;
    private final RepoIncomingMessage message;
    private LocalDateTime finishedAt;

    public RepoInPerfMessageWrapper(RepoIncomingMessage message) {
        this.message = message;
    }

    public void start() {
        if (null == startedAt) {
            startedAt = LocalTime.now().atDate(LocalDate.now());
        }
    }

    public void finish(LocalDateTime localDateTime) {
        if (null == finishedAt) {
            finishedAt = localDateTime;
        }
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
