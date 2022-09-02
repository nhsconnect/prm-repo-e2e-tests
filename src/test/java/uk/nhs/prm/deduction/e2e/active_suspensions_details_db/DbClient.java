package uk.nhs.prm.deduction.e2e.active_suspensions_details_db;

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

    public GetItemResponse queryDbWithNhsNumber(String nhsNumber) {
        Map<String, AttributeValue> key = new HashMap<>();
        System.out.println("Querying active-suspensions-details db with nhsNumber.");
        key.put("nhs_number", AttributeValue.builder().s(nhsNumber).build());
        var getItemResponse = DynamoDbClient.builder().build().getItem((GetItemRequest.builder()
                .tableName(testConfiguration.getActiveSupensionsDetailsDb())
                .key(key)
                .build()));
        System.out.println("query response is: " + getItemResponse);
        return getItemResponse;
    }
}
