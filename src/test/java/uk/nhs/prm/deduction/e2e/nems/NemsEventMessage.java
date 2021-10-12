package uk.nhs.prm.deduction.e2e.nems;


public class NemsEventMessage {
    public static NemsEventMessage parseMessage(String messageBody) {
        return new NemsEventMessage(messageBody);
    }

    private final String messageBody;

    public NemsEventMessage(String messageBody) {
        this.messageBody = messageBody;
    }

    public String body() {
        return messageBody;
    }
}
