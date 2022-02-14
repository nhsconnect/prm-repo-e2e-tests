package uk.nhs.prm.deduction.e2e.models;


public class MofUpdatedMessage extends NonSensitiveDataMessage {


    public MofUpdatedMessage(String nemsMessageId, String status) {
        super(nemsMessageId, status);
    }
}
