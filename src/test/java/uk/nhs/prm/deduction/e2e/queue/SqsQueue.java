package uk.nhs.prm.deduction.e2e.queue;

public class SqsQueue {
    private SQSClient sqsClient;

    public SqsQueue(SQSClient sqsClient) {
        this.sqsClient = sqsClient;
    }

    public String readMessageBody(String queueUri) {
        return sqsClient.readMessageFrom(queueUri);
    }
}
