package uk.nhs.prm.e2etests.model.nems;


import uk.nhs.prm.e2etests.model.nems.NemsResolutionMessage;

public class MofUpdatedMessageNems extends NemsResolutionMessage {
    public MofUpdatedMessageNems(String nemsMessageId, String status) {
        super(nemsMessageId, status);
    }
}
