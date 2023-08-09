package uk.nhs.prm.e2etests.model.nems;

public class NoLongerSuspendedMessageNems extends NemsResolutionMessage {
    public NoLongerSuspendedMessageNems(String nemsMessageId, String status) {
        super(nemsMessageId, status);
    }
}