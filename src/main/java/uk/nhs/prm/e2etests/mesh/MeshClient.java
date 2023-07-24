package uk.nhs.prm.e2etests.mesh;

import uk.nhs.prm.e2etests.client.StackOverflowInsecureSSLContextLoader;
import org.springframework.beans.factory.annotation.Autowired;
import uk.nhs.prm.e2etests.property.MeshProperties;
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

@Component
public class MeshClient {
    private final StackOverflowInsecureSSLContextLoader contextLoader;
    private final MeshProperties meshProperties;
    private final AuthTokenGenerator authTokenGenerator;
    private final Gson gson;
    private final String mailBoxId;

    @Autowired
    public MeshClient(
        StackOverflowInsecureSSLContextLoader contextLoader,
        MeshProperties meshProperties,
        AuthTokenGenerator authTokenGenerator,
        Gson gson
    ) {
        this.contextLoader = contextLoader;
        this.meshProperties = meshProperties;
        this.mailBoxId = this.meshProperties.getMailboxId();
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
                        meshProperties.getClientCert(),
                        meshProperties.getClientKey()
                ))
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
