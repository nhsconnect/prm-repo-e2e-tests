package uk.nhs.prm.deduction.e2e.models;

import uk.nhs.prm.deduction.e2e.TestConfiguration;
import uk.nhs.prm.deduction.e2e.tests.Patient;

import java.util.UUID;

public final class EhrRequestMessageBuilder {
    private String nhsNumber;
    private String sourceGpOds;
    private String destinationGpOds;
    private String sourceGpAsid;
    private String destinationGpAsid;
    private UUID conversationId;
    private UUID messageId;

    public EhrRequestMessageBuilder() {
        withRandomlyGeneratedConversationId();
        withRandomlyGeneratedMessageId();
        withNhsNumber("9692842304");  // nhs number in test template file
        withEhrSourceGp(Gp2GpSystem.REPO_DEV);
        withEhrDestinationGp(Gp2GpSystem.TPP_PTL_INT);
    }

    public EhrRequestMessageBuilder withNhsNumber(String nhsNumber) {
        this.nhsNumber = nhsNumber;
        return this;
    }

    public EhrRequestMessageBuilder withPatient(Patient patient) {
        return withNhsNumber(patient.nhsNumber());
    }

    public EhrRequestMessageBuilder withEhrSourceAsRepo(TestConfiguration config) {
        Gp2GpSystem repoInEnv = Gp2GpSystem.repoInEnv(config);
        this.sourceGpOds = repoInEnv.odsCode();
        this.sourceGpAsid = repoInEnv.asidCode();
        return this;
    }

    public EhrRequestMessageBuilder withEhrSourceGp(Gp2GpSystem ehrSourceGp) {
        this.sourceGpOds = ehrSourceGp.odsCode();
        this.sourceGpAsid = ehrSourceGp.asidCode();
        return this;
    }

    public EhrRequestMessageBuilder withEhrDestinationGp(Gp2GpSystem ehrDestinationGp) {
        this.destinationGpOds = ehrDestinationGp.odsCode();
        this.destinationGpAsid = ehrDestinationGp.asidCode();
        return this;
    }

    public EhrRequestMessageBuilder withConversationId(UUID conversationId) {
        this.conversationId = conversationId;
        return this;
    }

    public EhrRequestMessageBuilder withRandomlyGeneratedConversationId() {
        conversationId = UUID.randomUUID();
        System.out.println("generated conversation id " + conversationId);
        System.out.flush();
        return this;
    }

    public EhrRequestMessageBuilder withMessageId(UUID messageId) {
        this.messageId = messageId;
        return this;
    }

    public EhrRequestMessageBuilder withRandomlyGeneratedMessageId() {
        messageId = UUID.randomUUID();
        System.out.println("generated message id " + messageId);
        System.out.flush();
        return this;
    }

    public EhrRequestMessage build() {
        return new EhrRequestMessage(nhsNumber, sourceGpOds, destinationGpOds, sourceGpAsid, destinationGpAsid, conversationId, messageId);
    }
}
