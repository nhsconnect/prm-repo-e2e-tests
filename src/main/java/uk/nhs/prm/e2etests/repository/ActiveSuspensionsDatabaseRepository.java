package uk.nhs.prm.e2etests.repository;

import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import uk.nhs.prm.e2etests.property.DatabaseProperties;
import uk.nhs.prm.e2etests.model.ActiveSuspensionsMessage;

import java.util.HashMap;
import java.util.Map;

@Component
public class ActiveSuspensionsDatabaseRepository {
    private final DatabaseProperties databaseProperties;

    public ActiveSuspensionsDatabaseRepository(DatabaseProperties databaseProperties) {
        this.databaseProperties = databaseProperties;
    }

    public GetItemResponse queryWithNhsNumber(String nhsNumber) {
        Map<String, AttributeValue> key = new HashMap<>();
        System.out.println("Querying active-suspensions db with nhsNumber.");
        key.put("nhs_number", AttributeValue.builder().s(nhsNumber).build());
        var getItemResponse = DynamoDbClient.builder().build().getItem((GetItemRequest.builder()
                .tableName(databaseProperties.getActiveSuspensionsDbName())
                .key(key)
                .build()));
        System.out.println("query response is: " + getItemResponse);
        return getItemResponse;
    }

    public void save(ActiveSuspensionsMessage activeSuspensionMessage) {
        Map<String, AttributeValue> item = new HashMap<>();

        item.put("nhs_number", AttributeValue.builder().s(activeSuspensionMessage.getNhsNumber()).build());
        item.put("previous_gp", AttributeValue.builder().s(activeSuspensionMessage.getPreviousOdsCode()).build());
        item.put("nems_last_updated_date", AttributeValue.builder().s(activeSuspensionMessage.getNemsLastUpdatedDate()).build());

        DynamoDbClient.builder().build().putItem(PutItemRequest.builder()
                .tableName(databaseProperties.getActiveSuspensionsDbName())
                .item(item)
                .build());

    }
}
