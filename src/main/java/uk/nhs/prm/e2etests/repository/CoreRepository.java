package uk.nhs.prm.e2etests.repository;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import uk.nhs.prm.e2etests.exception.NotFoundException;
import uk.nhs.prm.e2etests.model.database.CoreRecord;

import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

import static uk.nhs.prm.e2etests.utility.TestDataUtility.randomUppercaseUuidAsString;

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

    private PageIterable<CoreRecord> getFragmentsByMessageId(String inboundConversationId, String fragmentMessageId) {
        //find items with inbound ID and FRAGMENT at start of Layer
        final QueryConditional condition = QueryConditional.keyEqualTo(k -> k.partitionValue(inboundConversationId));
        final Expression filter = Expression.builder()
                .expression("InboundMessageId = :id")
                .expressionValues(Map.of(":id", AttributeValue.builder().s(fragmentMessageId).build()))
                .build();

        final QueryEnhancedRequest request = QueryEnhancedRequest.builder()
                .queryConditional(condition)
                .filterExpression(filter)
                .build();

        return table.query(request);
    }

    public void hardDeleteFragmentWithId(String inboundConversationId, String fragmentMessageId) {
        CoreRecord fragmentToDelete = getFragmentsByMessageId(inboundConversationId, fragmentMessageId).items().stream().findFirst()
                .orElseThrow(() -> new NotFoundException(fragmentMessageId));

        // Delete the item
        table.deleteItem(fragmentToDelete);
        System.out.println("Fragment with messageId" + fragmentToDelete.getInboundMessageId() + "has been deleted.");
    }

    public void softDeleteCore(String inboundConversationId, Instant instant) {
        final CoreRecord record = getCoreByInboundConversationId(inboundConversationId)
            .orElseThrow(() -> new NotFoundException(inboundConversationId));

        record.setDeletedAt((int) (instant.toEpochMilli() / 1000));
        table.updateItem(record);
    }

    public void editInboundMessageId(String inboundConversationId) {
        final CoreRecord record = getCoreByInboundConversationId(inboundConversationId)
                .orElseThrow(() -> new NotFoundException(inboundConversationId));

        record.setInboundMessageId(randomUppercaseUuidAsString());
        table.updateItem(record);
    }

    public void hardDeleteCore(String inboundConversationId) {
        final CoreRecord record = getCoreByInboundConversationId(inboundConversationId)
                .orElseThrow(() -> new NotFoundException(inboundConversationId));

        table.deleteItem(record);
    }
}
