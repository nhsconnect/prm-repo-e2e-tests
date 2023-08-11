package uk.nhs.prm.e2etests.repository;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import uk.nhs.prm.e2etests.model.database.ActiveSuspensionsRecord;
import org.springframework.beans.factory.annotation.Autowired;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import uk.nhs.prm.e2etests.property.DynamoDbProperties;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import org.springframework.stereotype.Component;
import lombok.extern.log4j.Log4j2;

import java.util.Optional;

@Log4j2
@Component
public class ActiveSuspensionsDatabaseRepository {
    private final DynamoDbTable<ActiveSuspensionsRecord> activeSuspensionsTable;

    @Autowired
    public ActiveSuspensionsDatabaseRepository(
            DynamoDbProperties databaseProperties,
            DynamoDbEnhancedClient dynamoDbEnhancedClient
    ) {
        final TableSchema<ActiveSuspensionsRecord> tableSchema =
                TableSchema.fromBean(ActiveSuspensionsRecord.class);
        this.activeSuspensionsTable = dynamoDbEnhancedClient.table(
                databaseProperties.getActiveSuspensionsDbName(),
                tableSchema);
    }

    public Optional<ActiveSuspensionsRecord> findByNhsNumber(String nhsNumber) {
        return Optional.ofNullable(
                activeSuspensionsTable.getItem(Key.builder().partitionValue(nhsNumber).build())
        );
    }

    public void save(ActiveSuspensionsRecord activeSuspensionsDynamoDbEntry) {
        activeSuspensionsTable.putItem(activeSuspensionsDynamoDbEntry);
    }
}