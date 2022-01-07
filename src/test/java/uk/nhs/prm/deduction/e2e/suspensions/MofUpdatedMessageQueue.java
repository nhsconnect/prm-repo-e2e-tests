package uk.nhs.prm.deduction.e2e.suspensions;

import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.model.Message;
import uk.nhs.prm.deduction.e2e.TestConfiguration;
import uk.nhs.prm.deduction.e2e.queue.SqsQueue;

import java.util.List;

@Component
public class MofUpdatedMessageQueue extends SuspensionMessageQueue{

    @Autowired
    public MofUpdatedMessageQueue(SqsQueue sqsQueue, TestConfiguration configuration) {
        super(sqsQueue, configuration.mofUpdatedQueueUri());
    }

    public boolean containsMessage(List<Message> messages, String nhsNumber) throws JSONException {
        for (Message message: messages) {
            if(MofUpdateMessage.parseMessage(message.body()).nhsNumber().contains(nhsNumber))
            {
                log("Message present on queue");
                sqsQueue.deleteMessage(queueUri,message);
                return true
                        ;}
        }
        return false;
    }
}
