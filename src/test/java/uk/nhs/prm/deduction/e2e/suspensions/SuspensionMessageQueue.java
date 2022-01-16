package uk.nhs.prm.deduction.e2e.suspensions;

import org.json.JSONException;
import software.amazon.awssdk.services.sqs.model.Message;
import uk.nhs.prm.deduction.e2e.queue.SqsQueue;

import java.util.List;

public class SuspensionMessageQueue {
    protected final SqsQueue sqsQueue;
    protected final String queueUri;

    public SuspensionMessageQueue(SqsQueue sqsQueue, String queueUri) {
        this.sqsQueue = sqsQueue;
        this.queueUri = queueUri;
    }
    public List<Message> readMessages() {
        log(String.format("** Reading message from %s", this.queueUri));
        List<Message> messages = sqsQueue.readAllMessages(this.queueUri);
        return messages;
    }
    public void deleteAllMessages(){
        sqsQueue.deleteAllMessage(queueUri);
    }


    public boolean containsMessage(List<Message> messages,String nhsNumber) throws JSONException {
        for (Message message: messages) {
            if(SuspensionMessage.parseMessage(message.body()).nhsNumber().contains(nhsNumber))
            {
                log("Message present on queue");
                sqsQueue.deleteMessage(queueUri,message);
                return true
                        ;}
        }
        return false;
    }


    public void log(String messageBody) {
        System.out.println(messageBody);
    }
}
