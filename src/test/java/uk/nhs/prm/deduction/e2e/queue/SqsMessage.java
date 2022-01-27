package uk.nhs.prm.deduction.e2e.queue;

public class SqsMessage {
    public final String sentTimestamp;
    public final String body;

    public SqsMessage(String body, String sentTimestamp) {
        this.body = body;
        this.sentTimestamp = sentTimestamp;
    }
}
