package uk.nhs.prm.deduction.e2e.models;

import com.google.gson.GsonBuilder;

import java.time.ZonedDateTime;
import java.util.UUID;

public class RepoIncomingMessage {
    private final String nhsNumber;
    private final UUID nemsMessageId;
    private final String sourceGp;
    private final String destinationGp;
    private final UUID conversationId;
    private final String nemsEventLastUpdated;

    public RepoIncomingMessage(String nhsNumber, UUID nemsMessageId, String sourceGp, String destinationGp, UUID conversationId, ZonedDateTime nemsEventLastUpdated) {
        this.nhsNumber = nhsNumber;
        this.nemsMessageId = nemsMessageId;
        this.sourceGp = sourceGp;
        this.destinationGp = destinationGp;
        this.conversationId = conversationId;
        this.nemsEventLastUpdated = nemsEventLastUpdated.toString();
    }

    public String getConversationIdAsString() {
        return conversationId.toString();
    }

    public String getNemsMessageIdAsString() {
        return nemsMessageId.toString();
    }


    public String toJsonString() {
        return new GsonBuilder()
                .disableHtmlEscaping()
                .create()
                .toJson(this);
    }

    public String conversationId() {
        return getConversationIdAsString();
    }
}
