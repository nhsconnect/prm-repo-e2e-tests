package uk.nhs.prm.e2etests.model.nems;


import uk.nhs.prm.e2etests.model.nems.NemsResolutionMessage;

public class DeceasedPatientMessageNems extends NemsResolutionMessage {
    public DeceasedPatientMessageNems(String nemsMessageId, String status) {
        super(nemsMessageId, status);
    }
}
