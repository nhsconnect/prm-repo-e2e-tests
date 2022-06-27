package uk.nhs.prm.deduction.e2e.models;

import com.google.gson.GsonBuilder;

import java.lang.reflect.Modifier;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;

public class RepoIncomingMessageBuilder {
    private final String TTP_DEV = "M85019";
    private final String REPO_DEV = "B85002";

    private String nhsNumber;
    private UUID nemsMessageId;
    private String nemsEventLastUpdated;
    private String sourceGp;
    private String destinationGp;
    private UUID conversationId;

    public RepoIncomingMessageBuilder withNhsNumber(String nhsNumber) {
        this.nhsNumber =  nhsNumber;
        return this;
    }

    public RepoIncomingMessageBuilder withRandomlyGeneratedNemsMessageId() {
        this.nemsMessageId =  UUID.randomUUID();
        return this;
    }

    public RepoIncomingMessageBuilder withNemsEventLastUpdatedToNow() {
        this.nemsEventLastUpdated = ZonedDateTime.now(ZoneOffset.ofHours(0)).toString();
        return this;
    }

    public RepoIncomingMessageBuilder withSourceGpSetToTpp() {
        this.sourceGp = TTP_DEV;
        return this;
    }

    public RepoIncomingMessageBuilder withDestinationGpSetToRepoDev() {
        this.destinationGp = REPO_DEV;
        return this;
    }

    public RepoIncomingMessageBuilder withRandomlyGeneratedConversationId() {
        this.conversationId = UUID.randomUUID();
        return this;
    }

    public String getConversationIdAsString() {
        return this.conversationId.toString();
    }

    public String toJsonString() {
        return new GsonBuilder()
                .disableHtmlEscaping()
                .excludeFieldsWithModifiers(Modifier.FINAL)
                .create()
                .toJson(this);
    }
}
