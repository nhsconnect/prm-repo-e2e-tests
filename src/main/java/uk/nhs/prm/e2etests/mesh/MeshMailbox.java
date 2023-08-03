package uk.nhs.prm.e2etests.mesh;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.nhs.prm.e2etests.client.MeshClient;
import uk.nhs.prm.e2etests.model.nems.NemsEventMessage;
import uk.nhs.prm.e2etests.property.MeshProperties;

@Log4j2
@Component
public class MeshMailbox {
    private final MeshProperties meshProperties;
    private final MeshClient meshClient;

    @Autowired
    public MeshMailbox(
        MeshProperties meshProperties,
        MeshClient meshClient
    ) {
        this.meshProperties = meshProperties;
        this.meshClient = meshClient;
    }

    public String sendMessage(NemsEventMessage message) {
        log.info("Attempting to send NEMS message: {}", message.toString());
        return meshClient.sendMessage(this.meshProperties.getMailboxServiceOutboxUrl(), message);
    }
}
