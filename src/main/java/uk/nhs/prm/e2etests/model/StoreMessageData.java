package uk.nhs.prm.e2etests.model;

import uk.nhs.prm.e2etests.model.StoreMessageAttributes;

import java.util.List;
import java.util.UUID;

public class StoreMessageData {

    public String type;
    public UUID id;
    public StoreMessageAttributes attributes;

    public StoreMessageData(UUID messageId, UUID conversationId, String nhsNumber, String messageType, List<UUID> fragmentMessageIds) {
        this.type = "messages";
        this.id = messageId;
        this.attributes = new StoreMessageAttributes(conversationId, nhsNumber, messageType, fragmentMessageIds);
    }
}
