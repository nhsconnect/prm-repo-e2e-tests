package uk.nhs.prm.deduction.e2e.queue;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import uk.nhs.prm.deduction.e2e.nems.NemsEventMessage;

import java.util.List;


public class SQSClient {
    public String readMessageFrom(String queueUrl) {

        log("** Creating SQS client to read message");

        SqsClient sqsClient = SqsClient.create();

        ReceiveMessageRequest receiveMessageRequest = ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .waitTimeSeconds(5)
                .maxNumberOfMessages(1)
                .build();

        List<Message> messages = sqsClient.receiveMessage(receiveMessageRequest).messages();
        log("** Read messages on the queue ");
        log("** No of messages on the queue are <this should be 1> "+messages.size());
        return messages.get(0).body();
    }

    private NemsEventMessage someNemsEvent(String nhsNumber) {
        return new NemsEventMessage("dummy message for nhs number: " + nhsNumber);
    }

    public void log(String messageBody, String messageValue) {
        System.out.println(String.format(messageBody, messageValue));
    }
    public void log(String messageBody) {
        System.out.println(messageBody);
    }
}