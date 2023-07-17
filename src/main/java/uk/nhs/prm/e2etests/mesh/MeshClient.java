package uk.nhs.prm.e2etests.mesh;

import uk.nhs.prm.e2etests.client.StackOverflowInsecureSSLContextLoader;
import org.springframework.beans.factory.annotation.Autowired;
import uk.nhs.prm.e2etests.configuration.MeshConfiguration;
import uk.nhs.prm.e2etests.mesh.auth.AuthTokenGenerator;
import uk.nhs.prm.e2etests.model.NemsEventMessage;
import org.springframework.stereotype.Component;
import com.google.gson.Gson;

import java.net.URISyntaxException;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest;
import java.net.http.HttpClient;
import java.io.IOException;
import java.net.URL;

import static uk.nhs.prm.e2etests.configuration.MeshConfiguration.Type.CLIENT_CERT;
import static uk.nhs.prm.e2etests.configuration.MeshConfiguration.Type.CLIENT_KEY;
import static uk.nhs.prm.e2etests.configuration.MeshConfiguration.Type.MAILBOX_ID;

@Component
public class MeshClient {
    private final StackOverflowInsecureSSLContextLoader contextLoader;
    private final MeshConfiguration meshConfiguration;
    private final AuthTokenGenerator authTokenGenerator;
    private final Gson gson;
    private final String mailBoxId;

    @Autowired
    public MeshClient(
        StackOverflowInsecureSSLContextLoader contextLoader,
        MeshConfiguration meshConfiguration,
        AuthTokenGenerator authTokenGenerator,
        Gson gson
    ) {
        this.contextLoader = contextLoader;
        this.meshConfiguration = meshConfiguration;
        this.mailBoxId = this.meshConfiguration.getValue(MAILBOX_ID);
        this.authTokenGenerator = authTokenGenerator;
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
                .header("Mex-To", mailBoxId)
                .header("Mex-From", mailBoxId)
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
