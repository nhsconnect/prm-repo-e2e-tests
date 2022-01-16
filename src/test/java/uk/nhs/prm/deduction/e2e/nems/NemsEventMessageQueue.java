package uk.nhs.prm.deduction.e2e.nems;

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
        log(String.format("** Reading all messages from %s", this.queueUri));
        List<Message> messages = sqsQueue.readAllMessages(this.queueUri);
        return messages;
    }

    // we need to talk about this there is a lot of non-obvious and wrongly placed stuff going on here
    // - it's ok - we just need to make sure that someone with more code experience is pairing on this
    // - passing the message that may or may not have been taken from this queue into the queue is
    //   just asking for trouble
    // - having a destructive side effect such as deletion of message within an apparently non-destructive
    //   query call is just asking for trouble
    // - if we don't format our code reasonably when we edit it it looks like we don't care... or we
    //   actually are not taking care
    // - specifically:
    //   - indentation should always be logical
    //   - variable names always start with a small letter - the IDE is helping by underlining these
    //   - include whitespace after commas
    public boolean containsMessage(List<Message> messages, String NemsMessage) {
        log("Checking if message is present in some messages i just got");
        for (Message message : messages) {
            if (message.body().contains(NemsMessage)) {
                log("Message present on queue - but not necessarily the one i'm about to delete it from: " + queueUri);
                sqsQueue.deleteMessage(queueUri, message); /// wth?
                return true
                        ;
            } // wtformatting?
        }
        return false;
    }

    public void deleteAllMessages() {
        sqsQueue.deleteAllMessage(queueUri);
    }

    public void log(String messageBody) {
        System.out.println(messageBody);
    }
}
