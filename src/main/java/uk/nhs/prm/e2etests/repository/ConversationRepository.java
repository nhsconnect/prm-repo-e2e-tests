package uk.nhs.prm.e2etests.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import uk.nhs.prm.e2etests.exception.NotFoundException;
import uk.nhs.prm.e2etests.model.database.ConversationRecord;

import static uk.nhs.prm.e2etests.utility.DateTimeUtility.getIsoTimestamp;

import java.time.Instant;
import java.util.Optional;

@Component
public class ConversationRepository {
    private final DynamoDbTable<ConversationRecord> table;
    private static final String CONVERSATION_LAYER = "CONVERSATION";

    @Autowired
    public ConversationRepository(
            @Value("${aws.configuration.databaseNames.transferTrackerDb}") String databaseName,
            DynamoDbEnhancedClient dynamoDbEnhancedClient
    ) {
        table = dynamoDbEnhancedClient.table(databaseName, TableSchema.fromBean(ConversationRecord.class));
    }

    public void save(ConversationRecord conversationRecord) {
        conversationRecord.setLayer(CONVERSATION_LAYER);

        String currentTimestamp = getIsoTimestamp();

        if (conversationRecord.getCreatedAt() == null) {
            conversationRecord.setCreatedAt(currentTimestamp);
        }
        conversationRecord.setUpdatedAt(currentTimestamp);

        table.putItem(conversationRecord);
    }

    public Optional<ConversationRecord> findByInboundConversationId(String inboundConversationId) {
        final Key key = Key.builder()
                .partitionValue(inboundConversationId)
                .sortValue(CONVERSATION_LAYER)
                .build();

        return Optional.ofNullable(table.getItem(key));
    }

    public void markConversationAsSoftDeleted(String inboundConversationId, Instant instant) {
        final ConversationRecord record = findByInboundConversationId(inboundConversationId)
            .orElseThrow(() -> new NotFoundException(inboundConversationId));

        record.setDeletedAt((int) instant.toEpochMilli());

        table.updateItem(record);
    }
}