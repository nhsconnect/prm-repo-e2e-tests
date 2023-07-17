package uk.nhs.prm.e2etests.mesh;

import org.springframework.beans.factory.annotation.Autowired;
import uk.nhs.prm.e2etests.configuration.MeshConfiguration;
import uk.nhs.prm.e2etests.exception.MeshMailboxException;
import uk.nhs.prm.e2etests.model.NemsEventMessage;
import org.springframework.stereotype.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URISyntaxException;
import java.io.IOException;

@Component
public class MeshMailbox {
    private final MeshConfiguration meshConfiguration;
    private final MeshClient meshClient;
    private static final Logger LOGGER = LogManager.getLogger(MeshMailbox.class);

    @Autowired
    public MeshMailbox(
        MeshConfiguration meshConfiguration,
        MeshClient meshClient
    ) {
        this.meshConfig = meshConfig;
        this.meshClient = meshClient;
    }

    public String postMessage(NemsEventMessage message) {
        try {
            LOGGER.info("Attempting to send NEMS message: {}", message.toString());

            return meshClient.sendMessage(
                this.meshConfiguration.getMailboxServiceOutboxUri(),
                message
            );
        } catch (IOException | InterruptedException | URISyntaxException exception) {
            throw new MeshMailboxException(exception.getMessage());
        }
    }
}
