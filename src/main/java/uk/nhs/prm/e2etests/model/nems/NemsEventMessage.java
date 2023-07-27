package uk.nhs.prm.e2etests.model.nems;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.UUID;

@Getter
@EqualsAndHashCode
public class NemsEventMessage {
    private final UUID id;
    private final String message;

    public NemsEventMessage(String id, String message) {
        this.id = UUID.fromString(id);
        this.message = message;
    }

    @Override
    public String toString() {
        return String.format("NemsEventMessage{id=%s, message='%s'}", id, message);
    }
}