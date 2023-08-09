package uk.nhs.prm.e2etests.model;

import lombok.Builder;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.time.LocalDateTime;
import java.util.Objects;

@DynamoDbBean
@Getter
@Builder
public class TransferTrackerDynamoDbEntry {

//    static final TableSchema<TransferTrackerDynamoDbEntry> customerTableSchema = TableSchema.fromBean(this.class);

    private static final String DEFAULT_TIMESTAMP = LocalDateTime.now() + "Z";
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

    @DynamoDbPartitionKey
    public String getConversationId() {
        return conversationId;
    }

//    private static final DynamoDbEnhancedClient dynamoDbEnhancedClient =
//            DynamoDbEnhancedClient.create();

//    private static final DynamoDbTable<TransferTrackerDynamoDbEntry> transferTrackerTable =
//            dynamoDbEnhancedClient.table("dev-ehr-transfer-service-transfer-tracker",
//                    TableSchema.fromBean(TransferTrackerDynamoDbEntry.class));
//
//    public static TransferTrackerDynamoDbEntry getEntryByConversationId(String conversationId) {
//        return transferTrackerTable.getItem(Key.builder().partitionValue(conversationId).build());
//    }

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