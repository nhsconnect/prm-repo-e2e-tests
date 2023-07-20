package uk.nhs.prm.e2etests.transfer_tracker_db;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import uk.nhs.prm.e2etests.TestConfiguration;

import java.util.HashMap;
import java.util.Map;

@Component
public class TransferTrackerDbClient {

    @Autowired
    TestConfiguration testConfiguration;

    public GetItemResponse queryDbWithConversationId(String conversationId) {
        Map<String, AttributeValue> key = new HashMap<>();
        System.out.println("Querying transfer tracker db with conversation id : "+conversationId);
        key.put("conversation_id", AttributeValue.builder().s(conversationId).build());
        var getItemResponse = DynamoDbClient.builder().build().getItem((GetItemRequest.builder()
                .tableName(testConfiguration.getTransferTrackerDb())
                .key(key)
                .build()));
        System.out.println("query response is: " + getItemResponse);
        return getItemResponse;
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
                .tableName(testConfiguration.getTransferTrackerDb())
                .item(item)
                .build()
        );

    }
}
