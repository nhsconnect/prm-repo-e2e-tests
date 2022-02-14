package uk.nhs.prm.deduction.e2e.mesh;

import org.json.JSONException;
import org.json.JSONObject;
import uk.nhs.prm.deduction.e2e.mesh.auth.AuthTokenGenerator;
import uk.nhs.prm.deduction.e2e.client.StackOverflowInsecureSSLContextLoader;
import uk.nhs.prm.deduction.e2e.nems.NemsEventMessage;

import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class MeshClient {

    private final MeshConfig meshConfig;
    private StackOverflowInsecureSSLContextLoader contextLoader;
    private AuthTokenGenerator authTokenGenerator;

    public MeshClient(MeshConfig meshConfig) {
        this.meshConfig = meshConfig;
        this.contextLoader = new StackOverflowInsecureSSLContextLoader();
        this.authTokenGenerator = new AuthTokenGenerator(meshConfig);
    }

    public String postMessage(String mailboxServiceUri, NemsEventMessage message) {
        try {
            HttpRequest.BodyPublisher messageBody = HttpRequest.BodyPublishers.ofString(message.body());
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URL(mailboxServiceUri).toURI())
                    .method("POST", messageBody)
                    .header("Authorization", authTokenGenerator.getAuthorizationToken())
                    .header("Content-Type", "application/octet-stream")
                    .header("Mex-LocalID", "Test")
                    .header("Mex-To", meshConfig.getMailboxId())
                    .header("Mex-From", meshConfig.getMailboxId())
                    .header("Mex-WorkflowID", "API-DOCS-TEST")
                    .build();

            HttpResponse<String> response = HttpClient.newBuilder()
                    .sslContext(contextLoader.getClientAuthSslContext(meshConfig.getClientCert(), meshConfig.getClientKey()))
                    .build()
                    .send(request, HttpResponse.BodyHandlers.ofString());


            return getMessageIdFromMessage(response.body());
        }
        catch (Exception e) {
            log("Exception posting message on mailbox %s", e.getMessage());
            return null;
        }
    }

    private String getMessageIdFromMessage(String responseBody) {
        String key = "messageID";
        Object value = getJsonValue(responseBody, key);
        return String.valueOf(value);
    }

    private Object getJsonValue(String json, String key) {
        try {
            return new JSONObject(json).get(key);
        }
        catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public void log(String messageBody, String messageValue) {
        System.out.println(String.format(messageBody, messageValue));
    }
}
