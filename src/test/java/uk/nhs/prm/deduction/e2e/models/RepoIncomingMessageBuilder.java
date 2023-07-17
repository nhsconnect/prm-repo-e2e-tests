package uk.nhs.prm.deduction.e2e.models;

import uk.nhs.prm.e2etests.TestConfiguration;
import uk.nhs.prm.e2etests.enumeration.Patient;

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

    public RepoIncomingMessageBuilder() {
        withRandomlyGeneratedNemsMessageId();
        withRandomlyGeneratedConversationId();
        withNemsEventLastUpdatedToNow();
    }

    public RepoIncomingMessageBuilder withNhsNumber(String nhsNumber) {
        this.nhsNumber =  nhsNumber;
        return this;
    }

    public RepoIncomingMessageBuilder withPatient(Patient patient) {
        return withNhsNumber(patient.nhsNumber());
    }

    public RepoIncomingMessageBuilder withRandomlyGeneratedNemsMessageId() {
        nemsMessageId =  UUID.randomUUID();
        return this;
    }

    public RepoIncomingMessageBuilder withNemsEventLastUpdatedToNow() {
        nemsEventLastUpdated = ZonedDateTime.now(ZoneOffset.ofHours(0));
        return this;
    }

    public RepoIncomingMessageBuilder withEhrSourceGp(Gp2GpSystem ehrSource) {
        sourceGp = ehrSource.odsCode();
        return this;
    }
    public RepoIncomingMessageBuilder withEhrSourceGpOdsCode(String ehrSource) {
        sourceGp = ehrSource;
        return this;
    }
    public RepoIncomingMessageBuilder withEhrDestinationGp(Gp2GpSystem ehrDestination) {
        destinationGp = ehrDestination.odsCode();
        return this;
    }

    public RepoIncomingMessageBuilder withEhrDestination(String ehrDestination) {
        destinationGp = ehrDestination;
        return this;
    }


    public RepoIncomingMessageBuilder withEhrDestinationAsRepo(TestConfiguration config) {
        return withEhrDestinationGp(Gp2GpSystem.repoInEnv(config));
    }

    public RepoIncomingMessageBuilder withRandomlyGeneratedConversationId() {
        conversationId = UUID.randomUUID();
        System.out.println("generated conversation id " + conversationId);
        System.out.flush();
        return this;
    }

    public RepoIncomingMessage build() {
        return new RepoIncomingMessage(nhsNumber, nemsMessageId, sourceGp, destinationGp, conversationId, nemsEventLastUpdated);
    }
}
