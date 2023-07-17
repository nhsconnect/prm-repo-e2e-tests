package uk.nhs.prm.e2etests.model;

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
