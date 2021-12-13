package uk.nhs.prm.deduction.e2e.nems;

import org.awaitility.core.ThrowingRunnable;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.model.Message;
import uk.nhs.prm.deduction.e2e.queue.SqsQueue;

import java.util.List;

@Component
public class NemsEventMessageQueue {

    private final SqsQueue sqsQueue;
    private final String queueUri;

    public NemsEventMessageQueue(SqsQueue sqsQueue, String queueUri) {
        this.sqsQueue = sqsQueue;
        this.queueUri = queueUri;
    }

    public List<Message> readMessages() {
        log(String.format("** Reading message from %s", this.queueUri));
        List<Message> messages = sqsQueue.readMessageBody(this.queueUri);
        return messages;
    }

    public boolean containsMessage(List<Message> messages,String NemsMessage) {

        for (Message message: messages) {
           if(message.body().contains(NemsMessage))
           {
               log("Message present on queue");
               sqsQueue.deleteMessage(queueUri,message);
               return true
                       ;}
        }
        return false;
    }
public void deleteAllMessages(){
    sqsQueue.deleteAllMessage(queueUri);
}

    public void log(String messageBody) {
        System.out.println(messageBody);
    }
}
