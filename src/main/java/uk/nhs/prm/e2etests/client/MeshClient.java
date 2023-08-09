package uk.nhs.prm.e2etests.client;

import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.nhs.prm.e2etests.exception.MeshClientException;
import uk.nhs.prm.e2etests.mesh.auth.AuthTokenGenerator;
import uk.nhs.prm.e2etests.model.nems.NemsEventMessage;
import uk.nhs.prm.e2etests.property.MeshProperties;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Component
public class MeshClient {
    private final SSLContextLoader contextLoader;
    private final MeshProperties meshProperties;
    private final AuthTokenGenerator authTokenGenerator;
    private final Gson gson;
    private final String mailBoxId;

    @Autowired
    public MeshClient(
        SSLContextLoader contextLoader,
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

    public String sendMessage(String mailboxServiceUri, NemsEventMessage message) {
        try {
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
        } catch (IOException | InterruptedException | URISyntaxException exception) {
            Thread.currentThread().interrupt();
            throw new MeshClientException(exception.getMessage());
        }
    }

    private String getMessageIdFromMessage(String response) {
        return gson
                .fromJson(response, NemsEventMessage.class)
                .getMessage();
    }
}
