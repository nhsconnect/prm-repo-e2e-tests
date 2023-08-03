package uk.nhs.prm.e2etests.model;

import lombok.Getter;

import java.util.Objects;

@Getter
public record TransferTrackerDynamoDbEntry(
        String conversationId,
        String largeEhrCoreMessageId ,
        String nemsMessageId,
        String nhsNumber,
        String sourceGp,
        String state,
        String nemsEventLastUpdated,
        String createdAt,
        String lastUpdatedAt
) {
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