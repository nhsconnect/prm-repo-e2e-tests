package uk.nhs.prm.deduction.e2e.transfer_tracker_db;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import software.amazon.awssdk.services.dynamodb.paginators.ScanIterable;
import uk.nhs.prm.deduction.e2e.TestConfiguration;

import java.util.HashMap;
import java.util.Map;

@Component
public class DbClient {

    @Autowired
    TestConfiguration testConfiguration;

    public GetItemResponse queryDbWithConversationId(String conversationId) {
        Map<String, AttributeValue> key = new HashMap<>();
        System.out.println("Querying transfer tracker db with conversation id : " + conversationId);
        key.put("conversation_id", AttributeValue.builder().s(conversationId).build());
        var getItemResponse = DynamoDbClient.builder().build().getItem((GetItemRequest.builder()
                .tableName(testConfiguration.getTransferTrackerDb())
                .key(key)
                .build()));
        System.out.println("query response is: " + getItemResponse);
        return getItemResponse;
    }

    public void runDbScan() {
        DynamoDbClient client = DynamoDbClient.builder().build();
       tryExample(client);

//        Map<String, AttributeValue> expressionAttributeValues =
//                new HashMap<>();
//        expressionAttributeValues.put(":val",    AttributeValue.builder().s("2022-08-08T11:39:01.280559Z").build());
//
//
//        ScanRequest scan = ScanRequest.builder().tableName(testConfiguration.getTransferTrackerDb()).filterExpression("date_time = :val").expressionAttributeValues(expressionAttributeValues).build();
//        ScanIterable response = client.scanPaginator(scan);
//        for(Map<String, AttributeValue> item :  response.items()){
//            System.out.println(item);
//        }


    }

    private void tryExample(DynamoDbClient client){

        Map<String, String> expressionAttributeName =
                new HashMap<>();
        expressionAttributeName.put("#is_active", "is_active");
        expressionAttributeName.put("#created_at", "created_at");

        Map<String, AttributeValue> expressionAttributeValues =
                new HashMap<>();
        expressionAttributeValues.put(":is_active_val", AttributeValue.builder().s("true").build());
        expressionAttributeValues.put(":created_at_val", AttributeValue.builder().s("2022-08-10T12:14:17.640260Z").build());

        QueryRequest request = QueryRequest.builder().indexName("IsActiveSecondaryIndex")
                .tableName(testConfiguration.getTransferTrackerDb())
                .keyConditionExpression("#is_active = :is_active_val")
                .filterExpression("#created_at < :created_at_val")
                .expressionAttributeNames(expressionAttributeName)
                .expressionAttributeValues(expressionAttributeValues)
                .build();

        QueryResponse response = client.query(request);
        System.out.println("query response is: " + response);
    }
}
