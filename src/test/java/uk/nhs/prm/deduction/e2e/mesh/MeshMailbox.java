package uk.nhs.prm.deduction.e2e.mesh;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.nhs.prm.deduction.e2e.TestConfiguration;
import uk.nhs.prm.deduction.e2e.nems.NemsEventMessage;

@Component
public class MeshMailbox {

    private final MeshConfig meshConfig;
    private final MeshClient meshClient;

    public MeshMailbox(@Autowired TestConfiguration testConfiguration) {
        this.meshConfig =  new MeshConfig(testConfiguration);
        this.meshClient = new MeshClient(meshConfig);
    }


    public String postMessage(NemsEventMessage message) throws Exception {
        String messageId = meshClient.postMessage(getMailboxServicOutboxeUri(), message);
        log("Posted messageId is " + messageId);
        return messageId;
    }
    private String getMailboxServicOutboxeUri() {
        return String.format("https://msg.intspineservices.nhs.uk/messageexchange/%s/outbox", meshConfig.getMailboxId());
    }
    public void log(String message) {
        System.out.println(message);
    }
}
