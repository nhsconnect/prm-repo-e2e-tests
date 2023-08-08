package uk.nhs.prm.e2etests.enumeration;

public enum MessageType {
    EHR_REQUEST("RCMR_IN010000UK05"),
    EHR_CORE("RCMR_IN030000UK06"),
    CONTINUE_REQUEST("COPC_IN000001UK01"),
    EHR_FRAGMENT("COPC_IN000001UK01"),
    ACKNOWLEDGEMENT("MCCI_IN010000UK13");

    public final String interactionId;

    MessageType(String interactionId) {
        this.interactionId = interactionId;
    }
}