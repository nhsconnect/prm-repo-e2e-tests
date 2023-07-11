package uk.nhs.prm.e2etests.nems;


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
