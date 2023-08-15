package uk.nhs.prm.e2etests.repository;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import org.springframework.beans.factory.annotation.Autowired;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import uk.nhs.prm.e2etests.model.database.TransferTrackerRecord;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import uk.nhs.prm.e2etests.property.DynamoDbProperties;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import org.springframework.stereotype.Component;
import lombok.extern.log4j.Log4j2;

import java.util.Optional;

@Log4j2
@Component
public class TransferTrackerDatabaseRepository {
    private final DynamoDbTable<TransferTrackerRecord> transferTrackerTable;

    @Autowired
    public TransferTrackerDatabaseRepository(
            DynamoDbProperties databaseProperties,
            DynamoDbEnhancedClient dynamoDbEnhancedClient
    ) {
        final TableSchema<TransferTrackerRecord> tableSchema =
                TableSchema.fromBean(TransferTrackerRecord.class);
        this.transferTrackerTable = dynamoDbEnhancedClient.table(
                databaseProperties.getTransferTrackerDbName(),
                tableSchema);
    }

    public void save(TransferTrackerRecord transferTrackerDynamoDbEntry) {
        // Update the conversation ID so that it can be saved into the Transfer Tracker DynamoDB.
        final String conversationId = transferTrackerDynamoDbEntry.getConversationId();
        transferTrackerDynamoDbEntry.setConversationId(conversationId.toLowerCase());

        transferTrackerTable.putItem(transferTrackerDynamoDbEntry);
    }

    public Optional<TransferTrackerRecord> findByConversationId(String conversationId) {
        return Optional.ofNullable(
                transferTrackerTable.getItem(Key.builder().partitionValue(conversationId.toLowerCase()).build())
        );
    }
}
