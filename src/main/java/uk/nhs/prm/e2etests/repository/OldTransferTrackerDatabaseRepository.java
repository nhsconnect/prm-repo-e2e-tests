package uk.nhs.prm.e2etests.repository;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import org.springframework.beans.factory.annotation.Autowired;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import uk.nhs.prm.e2etests.model.database.OldTransferTrackerRecord;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import uk.nhs.prm.e2etests.property.DynamoDbProperties;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import org.springframework.stereotype.Component;
import lombok.extern.log4j.Log4j2;

import java.util.Optional;

/**
 * @deprecated PRMT-4670 old Transfer Tracker DB logic - to be removed
 */
@Deprecated(since="2.0.0", forRemoval = true)
@Log4j2
@Component
public class OldTransferTrackerDatabaseRepository {
    private final DynamoDbTable<OldTransferTrackerRecord> oldTransferTrackerTable;

    @Autowired
    public OldTransferTrackerDatabaseRepository(
            DynamoDbProperties databaseProperties,
            DynamoDbEnhancedClient dynamoDbEnhancedClient
    ) {
        final TableSchema<OldTransferTrackerRecord> tableSchema =
                TableSchema.fromBean(OldTransferTrackerRecord.class);
        this.oldTransferTrackerTable = dynamoDbEnhancedClient.table(
                databaseProperties.getOldTransferTrackerDbName(),
                tableSchema);
    }

    public void save(OldTransferTrackerRecord oldTransferTrackerDynamoDbEntry) {
        // Update the conversation ID so that it can be saved into the Transfer Tracker DynamoDB.
        final String conversationId = oldTransferTrackerDynamoDbEntry.getConversationId();
        oldTransferTrackerDynamoDbEntry.setConversationId(conversationId.toLowerCase());

        oldTransferTrackerTable.putItem(oldTransferTrackerDynamoDbEntry);
    }

    public Optional<OldTransferTrackerRecord> findByConversationId(String conversationId) {
        return Optional.ofNullable(
                oldTransferTrackerTable.getItem(Key.builder().partitionValue(conversationId.toLowerCase()).build())
        );
    }
}
