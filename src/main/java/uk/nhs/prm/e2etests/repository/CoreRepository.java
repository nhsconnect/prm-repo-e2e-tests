package uk.nhs.prm.e2etests.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import uk.nhs.prm.e2etests.exception.NotFoundException;
import uk.nhs.prm.e2etests.model.database.CoreRecord;

import java.time.Instant;
import java.util.Optional;

@Component
public class CoreRepository {
    private final DynamoDbTable<CoreRecord> table;
    private static final String CORE_LAYER = "CORE";

    @Autowired
    public CoreRepository(
        @Value("${aws.configuration.databaseNames.transferTrackerDb}") String tableName,
        DynamoDbEnhancedClient dynamoDbEnhancedClient
    ) {
        this.table = dynamoDbEnhancedClient.table(tableName, TableSchema.fromBean(CoreRecord.class));
    }

    private Optional<CoreRecord> getCoreByInboundConversationId(String inboundConversationId) {
        final Key key = Key.builder()
            .partitionValue(inboundConversationId)
            .sortValue(CORE_LAYER)
            .build();

        return Optional.ofNullable(table.getItem(key));
    }

    public void softDeleteCore(String inboundConversationId, Instant instant) {
        final CoreRecord record = getCoreByInboundConversationId(inboundConversationId)
            .orElseThrow(() -> new NotFoundException(inboundConversationId));

        record.setDeletedAt((int) instant.toEpochMilli());

        table.updateItem(record);
    }
}