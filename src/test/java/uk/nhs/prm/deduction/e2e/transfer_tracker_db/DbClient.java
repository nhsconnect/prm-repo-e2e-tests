package uk.nhs.prm.deduction.e2e.transfer_tracker_db;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import uk.nhs.prm.deduction.e2e.TestConfiguration;

import java.util.HashMap;
import java.util.Map;

@Component
public class DbClient {

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
}
