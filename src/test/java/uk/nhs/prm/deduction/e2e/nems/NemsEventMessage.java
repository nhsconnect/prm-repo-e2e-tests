package uk.nhs.prm.deduction.e2e.nems;


public class NemsEventMessage {
    public static NemsEventMessage parseMessage(String messageBody) {
        return new NemsEventMessage();
    }

    public String nhsNumber() {
        return "bob";
    }
}
