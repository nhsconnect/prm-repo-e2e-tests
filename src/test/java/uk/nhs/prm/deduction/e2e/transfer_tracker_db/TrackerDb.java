package uk.nhs.prm.deduction.e2e.transfer_tracker_db;


import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.IsEqual.equalTo;

@Component
public class TrackerDb {

    TransferTrackerDbClient transferTrackerDbClient;

    public TrackerDb(TransferTrackerDbClient transferTrackerDbClient) {
        this.transferTrackerDbClient = transferTrackerDbClient;
    }

    public boolean conversationIdExists(String conversationId) {
        var response = transferTrackerDbClient.queryDbWithConversationId(conversationId);
        if (response != null) {
            return true;
        }
        return false;
    }

    public boolean statusForConversationIdIs(String conversationId, String status) {
        return statusForConversationIdIs(conversationId, status, 120);
    }

    public boolean statusForConversationIdIs(String conversationId, String status, long timeout) {
        var defaultState = AttributeValue.builder().s("NOPE-AKA-DEFAULT-VALUE-TO-AVOID-NULL-EXCEPTION").build();
        await().atMost(timeout, TimeUnit.SECONDS)
                .with()
                .pollInterval(2, TimeUnit.SECONDS)
                .until(() -> transferTrackerDbClient.queryDbWithConversationId(conversationId)
                        .item().getOrDefault("state", defaultState).s(), equalTo(status));
        return true;
    }

    public String waitForStatusMatching(String conversationId, String partialStatus) {
        return await().atMost(120, TimeUnit.SECONDS)
                .with()
                .pollInterval(2, TimeUnit.SECONDS)
                .until(() -> transferTrackerDbClient.queryDbWithConversationId(conversationId).item().get("state").s(), containsString(partialStatus));
    }

    public void save(TransferTrackerDbMessage message) {
        transferTrackerDbClient.save(message);
    }
}
