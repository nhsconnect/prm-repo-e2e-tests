package uk.nhs.prm.deduction.e2e.transfer_tracker_db;


import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.containsString;
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

    public boolean statusForConversationIdIs(String conversationId, String status, boolean isActive) {
        await().atMost(120, TimeUnit.SECONDS)
                .with()
                .pollInterval(2, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    var dbResponse = dbClient.queryDbWithConversationId(conversationId);
                    var isActiveAttributeExistsInDb = dbResponse.item().get("is_active") != null;
                    assertThat(dbResponse.item().get("state").s()).isEqualTo(status);
                    assertThat(isActiveAttributeExistsInDb).isEqualTo(isActive);
                });
        return true;
    }


    public String waitForStatusMatching(String conversationId, String partialStatus) {
        return await().atMost(120, TimeUnit.SECONDS)
                .with()
                .pollInterval(2, TimeUnit.SECONDS)
                .until(() -> dbClient.queryDbWithConversationId(conversationId).item().get("state").s(), containsString(partialStatus));
    }
}
