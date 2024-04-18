package uk.nhs.prm.e2etests.model.database;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Objects;


/**
 * @deprecated PRMT-4670 old Transfer Tracker DB logic - to be removed
 */
@Deprecated(since="2.0.0")
@Setter
@Builder
@DynamoDbBean
@AllArgsConstructor
@NoArgsConstructor(force = true)
public class OldTransferTrackerRecord {
    private static final String DEFAULT_TIMESTAMP = LocalDateTime.now() + "Z";
    private String conversationId;
    @Builder.Default
    private String largeEhrCoreMessageId = "";
    private String nemsMessageId;
    private String nhsNumber;
    private String sourceGp;
    private String state;
    @Builder.Default
    private String nemsEventLastUpdated = DEFAULT_TIMESTAMP;
    @Builder.Default
    private String createdAt = DEFAULT_TIMESTAMP;
    @Builder.Default
    private String lastUpdatedAt = DEFAULT_TIMESTAMP;

    // GETTERS
    @DynamoDbPartitionKey()
    @DynamoDbAttribute("conversation_id")
    public String getConversationId() {
        return conversationId;
    }

    @DynamoDbAttribute("nems_message_id")
    public String getNemsMessageId() {
        return nemsMessageId;
    }

    @DynamoDbAttribute("nhs_number")
    public String getNhsNumber() {
        return nhsNumber;
    }

    @DynamoDbAttribute("source_gp")
    public String getSourceGp() {
        return sourceGp;
    }

    @DynamoDbAttribute("nems_event_last_updated")
    public String getNemsEventLastUpdated() {
        return nemsEventLastUpdated;
    }

    @DynamoDbAttribute("created_at")
    public String getCreatedAt() {
        return createdAt;
    }

    @DynamoDbAttribute("last_updated_at")
    public String getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    @DynamoDbAttribute("large_ehr_core_message_id")
    public String getLargeEhrCoreMessageId() {
        return largeEhrCoreMessageId;
    }

    @DynamoDbAttribute("state")
    public String getState() {
        return state;
    }

    // EQUALS AND HASHCODE
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OldTransferTrackerRecord that = (OldTransferTrackerRecord) o;
        return Objects.equals(conversationId, that.conversationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(conversationId);
    }
}