package uk.nhs.prm.deduction.e2e.transfer_tracker_db;


import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.core.IsEqual.equalTo;

@Component
public class TrackerDb {

    DbClient dbClient;

    public TrackerDb(DbClient dbClient) {
        this.dbClient = dbClient;
    }

    public boolean conversationIdExists(String conversationId) {
        var response = dbClient.queryDbWithConversationId(conversationId);
        if (response != null) {
            return true;
        }
        return false;
    }

    public boolean statusForConversationIdIs(String conversationId, String status) {
        await().atMost(120, TimeUnit.SECONDS)
                .with()
                .pollInterval(2, TimeUnit.SECONDS)
                .until(() -> dbClient.queryDbWithConversationId(conversationId).item().get("state").s(), equalTo(status));
        return true;
    }


}
