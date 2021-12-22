package uk.nhs.prm.deduction.e2e.mesh;

import org.apache.http.HttpException;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.nhs.prm.deduction.e2e.TestConfiguration;
import uk.nhs.prm.deduction.e2e.nems.NemsEventMessage;

import java.util.List;

@Component
public class MeshMailbox {

    @Autowired
    private TestConfiguration configuration;
    @Autowired
    private MeshClient meshClient;

    public String postMessage(NemsEventMessage message) throws Exception {
        String messageId = meshClient.postMessage(getMailboxServicOutboxeUri(), message);
        log("Posted messageId is " + messageId);
        return messageId;
    }

    public boolean hasMessageId(String messageId) throws HttpException, JSONException {
        List<String> messageIds = meshClient.getMessageIds(getMailboxServicInboxeUri());

        return listContainsMessageID(messageIds,messageId);
    }


    private String getMailboxServicOutboxeUri() {
        return String.format("https://msg.intspineservices.nhs.uk/messageexchange/%s/outbox", configuration.getMeshMailBoxID());
    }

    private String getMailboxServicInboxeUri() {
        return String.format("https://msg.intspineservices.nhs.uk/messageexchange/%s/inbox", configuration.getMeshMailBoxID());
    }

    private boolean listContainsMessageID(List<String> messageList, String messageId) throws JSONException {
        if(messageList.isEmpty()){
            return false;
        }
        for (int index = 0;index<messageList.size();index++){
            if(messageList.get(index).equalsIgnoreCase(messageId)){
                return true;
            }
        }
       return false;
    }

    public void log(String message) {
        System.out.println(message);
    }
}
