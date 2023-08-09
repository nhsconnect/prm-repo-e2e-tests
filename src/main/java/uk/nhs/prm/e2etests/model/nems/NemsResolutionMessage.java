package uk.nhs.prm.e2etests.model.nems;

import lombok.Getter;

import java.util.Objects;
import java.util.UUID;

@Getter
public class NemsResolutionMessage {
    private final UUID nemsMessageId;
    private final String status;

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof NemsResolutionMessage message)) return false;
        return nemsMessageId.equals(message.nemsMessageId) && status.equalsIgnoreCase(message.status);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nemsMessageId, status);
    }

    public NemsResolutionMessage(String nemsMessageId, String status) {
        this.nemsMessageId = UUID.fromString(nemsMessageId);
        this.status = status;
    }
}