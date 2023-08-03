package uk.nhs.prm.e2etests.service;

import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import uk.nhs.prm.e2etests.model.TransferTrackerDynamoDbEntry;
import uk.nhs.prm.e2etests.repository.TransferTrackerDatabaseRepository;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.IsEqual.equalTo;

@Service
public class TransferTrackerService {

    private final TransferTrackerDatabaseRepository transferTrackerDatabaseRepository;

    public TransferTrackerService(TransferTrackerDatabaseRepository transferTrackerDatabaseRepository) {
        this.transferTrackerDatabaseRepository = transferTrackerDatabaseRepository;
    }

    public boolean conversationIdExists(String conversationId) {
        GetItemResponse response = transferTrackerDatabaseRepository.queryWithConversationId(conversationId);
        return response != null;
    }

    public boolean statusForConversationIdIs(String conversationId, String status) {
        return statusForConversationIdIs(conversationId, status, 120);
    }

    public boolean statusForConversationIdIs(String conversationId, String status, long timeout) {
        AttributeValue defaultState = AttributeValue.builder().s("NOPE-AKA-DEFAULT-VALUE-TO-AVOID-NULL-EXCEPTION").build();
        await().atMost(timeout, TimeUnit.SECONDS)
                .with()
                .pollInterval(2, TimeUnit.SECONDS)
                .until(() -> transferTrackerDatabaseRepository.queryWithConversationId(conversationId)
                        .item().getOrDefault("state", defaultState).s(), equalTo(status));
        return true;
    }

    public String waitForStatusMatching(String conversationId, String partialStatus) {
        return await().atMost(120, TimeUnit.SECONDS)
                .with()
                .pollInterval(2, TimeUnit.SECONDS)
                .until(() -> transferTrackerDatabaseRepository.queryWithConversationId(conversationId).item().get("state").s(), containsString(partialStatus));
    }

    public void save(TransferTrackerDynamoDbEntry message) {
        transferTrackerDatabaseRepository.save(message);
    }
}
