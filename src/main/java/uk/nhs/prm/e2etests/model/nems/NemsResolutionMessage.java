package uk.nhs.prm.e2etests.model.nems;

import lombok.Getter;

import java.util.UUID;

@Getter
public class NemsResolutionMessage {
    private final UUID nemsMessageId;
    private final String status;

    public boolean hasTheSameContentAs(NemsResolutionMessage nemsResolutionMessage) {
        if (nemsResolutionMessage == null) return false;
        return nemsMessageId.equals(nemsResolutionMessage.nemsMessageId) && status.equalsIgnoreCase(nemsResolutionMessage.status);
    }

    public NemsResolutionMessage(String nemsMessageId, String status) {
        this.nemsMessageId = UUID.fromString(nemsMessageId);
        this.status = status;
    }
}