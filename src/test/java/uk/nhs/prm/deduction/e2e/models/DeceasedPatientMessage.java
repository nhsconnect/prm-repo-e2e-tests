package uk.nhs.prm.deduction.e2e.models;


public class DeceasedPatientMessage extends NonSensitiveDataMessage {


    public DeceasedPatientMessage(String nemsMessageId, String status) {
        super(nemsMessageId, status);
    }
}
