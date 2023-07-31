package uk.nhs.prm.e2etests.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import uk.nhs.prm.e2etests.model.TransferTrackerDbMessage;
import uk.nhs.prm.e2etests.property.DatabaseProperties;

import java.util.HashMap;
import java.util.Map;

@Component
public class TransferTrackerDatabaseRepository {
    private final DatabaseProperties databaseProperties;

    private final AwsCredentialsProvider awsCredentialsProvider;

    @Autowired
    public TransferTrackerDatabaseRepository(
            DatabaseProperties databaseProperties,
            AwsCredentialsProvider awsCredentialsProvider
    ) {
        this.databaseProperties = databaseProperties;
        this.awsCredentialsProvider = awsCredentialsProvider;
    }

    public GetItemResponse queryWithConversationId(String conversationId) {
        System.out.println("Querying transfer tracker db with conversation id : "+conversationId);

        final Map<String, AttributeValue> dynamoDbKey = Map.of(
        "conversation_id", AttributeValue.builder().s(conversationId).build()
        );

        try(DynamoDbClient dynamoDbClient = DynamoDbClient.builder()
                .credentialsProvider(awsCredentialsProvider)
                .region(Region.EU_WEST_2)
                .build()) {

            return dynamoDbClient.getItem((GetItemRequest.builder()
                .tableName(databaseProperties.getTransferTrackerDbName())
                .key(dynamoDbKey)
                .build()));
        }
    }

    public void save(TransferTrackerDbMessage transferTrackerDbMessage) {
        Map<String, AttributeValue> item = new HashMap<>();

        item.put("conversation_id", AttributeValue.builder().s(transferTrackerDbMessage.getConversationId()).build());
        item.put("large_ehr_core_message_id", AttributeValue.builder().s(transferTrackerDbMessage.getLargeEhrCoreMessageId()).build());
        item.put("nems_message_id", AttributeValue.builder().s(transferTrackerDbMessage.getNemsMessageId()).build());
        item.put("nhs_number", AttributeValue.builder().s(transferTrackerDbMessage.getNhsNumber()).build());
        item.put("source_gp", AttributeValue.builder().s(transferTrackerDbMessage.getSourceGp()).build());
        item.put("state", AttributeValue.builder().s(transferTrackerDbMessage.getState()).build());
        item.put("nems_event_last_updated", AttributeValue.builder().s(transferTrackerDbMessage.getNemsEventLastUpdated()).build());
        item.put("created_at", AttributeValue.builder().s(transferTrackerDbMessage.getCreatedAt()).build());
        item.put("last_updated_at", AttributeValue.builder().s(transferTrackerDbMessage.getLastUpdatedAt()).build());


        DynamoDbClient.builder().build().putItem(PutItemRequest.builder()
                .tableName(databaseProperties.getTransferTrackerDbName())
                .item(item)
                .build()
        );

    }
}
