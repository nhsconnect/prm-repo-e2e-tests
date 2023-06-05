package uk.nhs.prm.deduction.e2e.services.ehr_repo;


import java.util.List;
import java.util.UUID;

public class StoreMessageAttributes {
    public UUID conversationId;
    public String messageType;
    public String nhsNumber;
    public List<UUID> fragmentMessageIds;

    public StoreMessageAttributes(UUID conversationId, String nhsNumber, String messageType, List<UUID> fragmentMessageIds) {
        this.conversationId = conversationId;
        this.messageType = messageType;
        this.nhsNumber = nhsNumber;
        this.fragmentMessageIds = fragmentMessageIds;
    }
}
