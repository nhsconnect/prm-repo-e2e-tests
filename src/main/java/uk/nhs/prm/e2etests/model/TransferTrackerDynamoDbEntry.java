package uk.nhs.prm.e2etests.model;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Objects;

@Getter
@Builder
public class TransferTrackerDynamoDbEntry {
    private static final String DEFAULT_TIMESTAMP = LocalDateTime.now().toString() + "Z"; // before was ZonedDateTime.now(ZoneOffset.ofHours(0)).toString()
    private final String conversationId;
    private final String largeEhrCoreMessageId;
    private final String nemsMessageId;
    private final String nhsNumber;
    private final String sourceGp;
    private final String state;
    @Builder.Default
    private final String nemsEventLastUpdated = DEFAULT_TIMESTAMP;
    @Builder.Default
    private final String createdAt = DEFAULT_TIMESTAMP;
    @Builder.Default
    private final String lastUpdatedAt = DEFAULT_TIMESTAMP;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TransferTrackerDynamoDbEntry that = (TransferTrackerDynamoDbEntry) o;
        return Objects.equals(conversationId, that.conversationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(conversationId);
    }
}