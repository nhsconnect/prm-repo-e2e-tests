package uk.nhs.prm.deduction.e2e.models;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;

public class RepoIncomingMessageBuilder {
    private String nhsNumber;
    private UUID nemsMessageId;
    private ZonedDateTime nemsEventLastUpdated;
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
        this.nemsEventLastUpdated = ZonedDateTime.now(ZoneOffset.ofHours(0));
        return this;
    }

    public RepoIncomingMessageBuilder withSourceGpSetToTpp() {
        this.sourceGp = Gp2GpSystem.TTP_DEV.odsCode();
        return this;
    }

    public RepoIncomingMessageBuilder withDestinationGpSetToRepoDev() {
        this.destinationGp = Gp2GpSystem.REPO_DEV.odsCode();
        return this;
    }

    public RepoIncomingMessageBuilder withRandomlyGeneratedConversationId() {
        this.conversationId = UUID.randomUUID();
        return this;
    }

    public RepoIncomingMessage build() {
        return new RepoIncomingMessage(nhsNumber, nemsMessageId, sourceGp, destinationGp, conversationId, nemsEventLastUpdated);
    }
}
