package uk.nhs.prm.e2etests.model.nems;

import lombok.Getter;

@Getter
public class NemsResolutionMessage {
    String nemsMessageId;
    String status;

    public NemsResolutionMessage(String nemsMessageId, String status) {
        this.nemsMessageId = nemsMessageId;
        this.status = status;
    }
}
