package uk.nhs.prm.e2etests.model.nems;

import lombok.Getter;

import java.util.UUID;

@Getter
public class NemsResolutionMessage {
    private final UUID id;
    private final String status;

    public boolean hasTheSameContentAs(NemsResolutionMessage nemsResolutionMessage) {
        if (nemsResolutionMessage == null) return false;
        return id.equals(nemsResolutionMessage.id) && status.equalsIgnoreCase(nemsResolutionMessage.status);
    }

    public NemsResolutionMessage(String id, String status) {
        this.id = UUID.fromString(id);
        this.status = status;
    }
}