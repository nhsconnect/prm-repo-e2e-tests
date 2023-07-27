package uk.nhs.prm.e2etests.model.nems;


import uk.nhs.prm.e2etests.model.nems.NemsResolutionMessage;

public class NoLongerSuspendedMessageNems extends NemsResolutionMessage {

    public NoLongerSuspendedMessageNems(String nemsMessageId, String status) {
        super(nemsMessageId, status);
    }
}
