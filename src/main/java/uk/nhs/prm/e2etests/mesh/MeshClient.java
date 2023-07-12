package uk.nhs.prm.e2etests.mesh;

import com.google.gson.Gson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.nhs.prm.e2etests.client.StackOverflowInsecureSSLContextLoader;
import uk.nhs.prm.e2etests.configuration.MeshConfiguration;
import uk.nhs.prm.e2etests.model.NemsEventMessage;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static uk.nhs.prm.e2etests.configuration.MeshConfiguration.Type.CLIENT_CERT;
import static uk.nhs.prm.e2etests.configuration.MeshConfiguration.Type.CLIENT_KEY;

/**
 * The MeshClient provides an API which allows developers to interact with
 * the Mesh Mailbox. You can use this component to send and receive messages
 * to / from the mailbox.
 */
@Component
public class MeshClient {
    private final Logger LOGGER = LogManager.getLogger(MeshClient.class);
    private final StackOverflowInsecureSSLContextLoader contextLoader;
    private final MeshConfiguration meshConfiguration;
    private final Gson gson;

    @Autowired
    public MeshClient(
        StackOverflowInsecureSSLContextLoader contextLoader,
        MeshConfiguration meshConfiguration,
        Gson gson
    ) {
        this.contextLoader = contextLoader;
        this.meshConfiguration = meshConfiguration;
        this.gson = gson;
    }

    public String sendMessage(String mailboxServiceUri, NemsEventMessage message) throws IOException, InterruptedException, URISyntaxException {
        final HttpRequest.BodyPublisher messageBody = HttpRequest
                .BodyPublishers
                .ofString(message.getMessage());

        final HttpRequest request = HttpRequest.newBuilder()
                .uri(new URL(mailboxServiceUri).toURI())
                .method("POST", messageBody)
                .header("Authorization", authTokenGenerator.getAuthorizationToken())
                .header("Content-Type", "application/octet-stream")
                .header("Mex-LocalID", "Test")
                .header("Mex-To", meshConfig.getMailboxId())
                .header("Mex-From", meshConfig.getMailboxId())
                .header("Mex-WorkflowID", "API-DOCS-TEST")
                .build();

        final HttpResponse<String> response = HttpClient.newBuilder()
                .sslContext(contextLoader.getClientAuthSslContext(
                        meshConfiguration.getValue(CLIENT_CERT),
                        meshConfiguration.getValue(CLIENT_KEY))
                )
                .build()
                .send(request, HttpResponse.BodyHandlers.ofString());

        return getMessageIdFromMessage(response.body());
    }

    private String getMessageIdFromMessage(String response) {
        return gson
                .fromJson(response, NemsEventMessage.class)
                .getMessage();
    }
}
