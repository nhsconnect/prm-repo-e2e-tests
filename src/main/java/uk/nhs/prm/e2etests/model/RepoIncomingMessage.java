package uk.nhs.prm.e2etests.model;

import com.google.gson.GsonBuilder;
import lombok.Getter;

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.UUID;

@Getter
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

    public String getConversationId() {
        return conversationId.toString();
    }

    public String toJsonString() {
        return new GsonBuilder().disableHtmlEscaping()
                .create()
                .toJson(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RepoIncomingMessage that = (RepoIncomingMessage) o;
        return Objects.equals(nemsMessageId, that.nemsMessageId) && Objects.equals(conversationId, that.conversationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nemsMessageId, conversationId);
    }
}