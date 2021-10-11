package uk.nhs.prm.deduction.e2e.mesh;

import uk.nhs.prm.deduction.e2e.auth.AuthTokenGenerator;
import org.apache.http.HttpException;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;

import static uk.nhs.prm.deduction.e2e.client.StackOverflowInsecureSSLContextLoader.getClientAuthSslContext;

public class MeshClient {

    @Value("${meshMailboxId}")
    String meshMailboxId;

    public void postMessageToMeshMailbox() throws HttpException {
        try {
            System.out.println("Getting value from SSM "+meshMailboxId);

            AuthTokenGenerator authTokenGenerator = new AuthTokenGenerator();

            String token = authTokenGenerator.getAuthorizationToken();

            String payloadString = "Test-String";
            String endpoint = "";
            HttpRequest.BodyPublisher jsonPayload = HttpRequest.BodyPublishers.ofString(payloadString);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URL(endpoint).toURI())
                    .method("POST", jsonPayload)
                    .header("Authorization", token)
                    .header("Content-Type", "application/octet-stream")
                    .header("Mex-LocalID", "Test")
                    .header("Mex-To", meshMailboxId)
                    .header("Mex-From", meshMailboxId)
                    .header("Mex-WorkflowID", "API-DOCS-TEST")
                    .build();

            HttpResponse<String> response = HttpClient.newBuilder()
                    .sslContext(getClientAuthSslContext(
                            readTestResource("mesh-mailbox-client-cert.pem"),
                            readTestResource("mesh-mailbox-key.pem")))
                    .build()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("response status code from mesh is " + response.statusCode());
            System.out.println("response body from mesh is " + response.body());

        } catch (Exception e) {
            throw new HttpException("Exception encountered", e);
        }
    }

    private String readTestResource(String filename) throws IOException {
        return Files.readString(Paths.get("src/test/resources/" + filename));
    }
}
