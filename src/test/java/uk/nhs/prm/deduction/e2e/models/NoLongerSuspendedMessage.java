package uk.nhs.prm.deduction.e2e.models;


public class NoLongerSuspendedMessage extends NonSensitiveDataMessage {

    public NoLongerSuspendedMessage(String nemsMessageId, String status) {
        super(nemsMessageId, status);
    }
}
