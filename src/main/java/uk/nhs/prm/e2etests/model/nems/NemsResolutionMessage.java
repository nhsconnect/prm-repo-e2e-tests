package uk.nhs.prm.e2etests.model.nems;

import lombok.Getter;

import java.util.Objects;

@Getter
public class NemsResolutionMessage {
    String nemsMessageId;
    String status;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NemsResolutionMessage that = (NemsResolutionMessage) o;
        return Objects.equals(nemsMessageId, that.nemsMessageId) && Objects.equals(status, that.status);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nemsMessageId, status);
    }

    public NemsResolutionMessage(String nemsMessageId, String status) {
        this.nemsMessageId = nemsMessageId;
        this.status = status;
    }
}
