package uk.nhs.prm.e2etests.repository;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import org.springframework.beans.factory.annotation.Autowired;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import uk.nhs.prm.e2etests.model.TransferTrackerDynamoDbEntry;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import uk.nhs.prm.e2etests.property.DatabaseProperties;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import org.springframework.stereotype.Component;
import lombok.extern.log4j.Log4j2;

import java.util.Optional;

@Log4j2
@Component
public class TransferTrackerDatabaseRepository {
    private final DynamoDbTable<TransferTrackerDynamoDbEntry> transferTrackerTable;

    @Autowired
    public TransferTrackerDatabaseRepository(
            DatabaseProperties databaseProperties,
            DynamoDbEnhancedClient dynamoDbEnhancedClient
    ) {
        final TableSchema<TransferTrackerDynamoDbEntry> tableSchema =
                TableSchema.fromBean(TransferTrackerDynamoDbEntry.class);
        this.transferTrackerTable = dynamoDbEnhancedClient.table(
                databaseProperties.getTransferTrackerDbName(),
                tableSchema);
    }

    public void save(TransferTrackerDynamoDbEntry transferTrackerDynamoDbEntry) {
        transferTrackerTable.putItem(transferTrackerDynamoDbEntry);
    }

    public Optional<TransferTrackerDynamoDbEntry> findByConversationId(String conversationId) {
        return Optional.ofNullable(
                transferTrackerTable.getItem(Key.builder().partitionValue(conversationId).build())
        );
    }
}
