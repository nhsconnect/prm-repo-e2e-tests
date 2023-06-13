package uk.nhs.prm.deduction.e2e.models;

import uk.nhs.prm.deduction.e2e.TestConfiguration;

import java.util.UUID;

public final class ContinueRequestMessageBuilder {
    private UUID conversationId;
    private UUID messageId;
    private String sourceGpOds;
    private String destinationGpOds;
    private String sourceGpAsid;
    private String destinationGpAsid;

    public ContinueRequestMessageBuilder() {
        withRandomlyGeneratedConversationId();
        withRandomlyGeneratedMessageId();
        withEhrSourceGp(Gp2GpSystem.REPO_DEV);
        withEhrDestinationGp(Gp2GpSystem.TPP_PTL_INT);
    }

    public ContinueRequestMessageBuilder withConversationId(UUID conversationId) {
        this.conversationId = conversationId;
        return this;
    }

    public ContinueRequestMessageBuilder withRandomlyGeneratedConversationId() {
        conversationId = UUID.randomUUID();
        System.out.println("generated conversation id " + conversationId);
        System.out.flush();
        return this;
    }

    public ContinueRequestMessageBuilder withEhrSourceAsRepo(TestConfiguration config) {
        Gp2GpSystem repoInEnv = Gp2GpSystem.repoInEnv(config);
        this.sourceGpOds = repoInEnv.odsCode();
        this.sourceGpAsid = repoInEnv.asidCode();
        return this;
    }

    public ContinueRequestMessageBuilder withEhrSourceGp(Gp2GpSystem ehrSourceGp) {
        this.sourceGpOds = ehrSourceGp.odsCode();
        this.sourceGpAsid = ehrSourceGp.asidCode();
        return this;
    }

    public ContinueRequestMessageBuilder withEhrDestinationGp(Gp2GpSystem ehrDestinationGp) {
        this.destinationGpOds = ehrDestinationGp.odsCode();
        this.destinationGpAsid = ehrDestinationGp.asidCode();
        return this;
    }

    public ContinueRequestMessageBuilder withMessageId(UUID messageId) {
        this.messageId = messageId;
        return this;
    }

    public ContinueRequestMessageBuilder withRandomlyGeneratedMessageId() {
        messageId = UUID.randomUUID();
        System.out.println("generated message id " + messageId);
        System.out.flush();
        return this;
    }

    public ContinueRequestMessageBuilder withSourceGpOds(String sourceGpOds) {
        this.sourceGpOds = sourceGpOds;
        return this;
    }

    public ContinueRequestMessageBuilder withDestinationGpOds(String destinationGpOds) {
        this.destinationGpOds = destinationGpOds;
        return this;
    }

    public ContinueRequestMessageBuilder withSourceGpAsid(String sourceGpAsid) {
        this.sourceGpAsid = sourceGpAsid;
        return this;
    }

    public ContinueRequestMessageBuilder withDestinationGpAsid(String destinationGpAsid) {
        this.destinationGpAsid = destinationGpAsid;
        return this;
    }

    public ContinueRequestMessage build() {
        return new ContinueRequestMessage(conversationId, messageId, sourceGpOds, destinationGpOds, sourceGpAsid, destinationGpAsid);
    }
}
