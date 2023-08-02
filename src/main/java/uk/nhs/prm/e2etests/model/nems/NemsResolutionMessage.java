package uk.nhs.prm.e2etests.model.nems;

import lombok.Getter;

@Getter
public class NemsResolutionMessage {
    String nemsMessageId;
    String status;

    public boolean hasTheSameContentAs(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        NemsResolutionMessage that = (NemsResolutionMessage) o;
        return nemsMessageId.equalsIgnoreCase(that.nemsMessageId) && status.equalsIgnoreCase(that.status);
    }

    public NemsResolutionMessage(String nemsMessageId, String status) {
        this.nemsMessageId = nemsMessageId;
        this.status = status;
    }
}