package uk.nhs.prm.deduction.e2e.mesh;

import org.apache.http.HttpException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.nhs.prm.deduction.e2e.TestConfiguration;
import uk.nhs.prm.deduction.e2e.auth.AuthTokenGenerator;
import uk.nhs.prm.deduction.e2e.client.StackOverflowInsecureSSLContextLoader;
import uk.nhs.prm.deduction.e2e.nems.NemsEventMessage;

import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

@Component
public class MeshClient {

    @Autowired
    private TestConfiguration configuration;

    @Autowired
    AuthTokenGenerator authTokenGenerator;

    private StackOverflowInsecureSSLContextLoader contextLoader;

    public MeshClient() {
        this.contextLoader = new StackOverflowInsecureSSLContextLoader();
    }

    public String postMessage(String mailboxServiceUri, NemsEventMessage message) throws Exception {
        try {
            HttpRequest.BodyPublisher messageBody = HttpRequest.BodyPublishers.ofString(message.body());
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URL(mailboxServiceUri).toURI())
                    .method("POST", messageBody)
                    .header("Authorization", getAuthToken())
                    .header("Content-Type", "application/octet-stream")
                    .header("Mex-LocalID", "Test")
                    .header("Mex-To", configuration.getMeshMailBoxID())
                    .header("Mex-From", configuration.getMeshMailBoxID())
                    .header("Mex-WorkflowID", "API-DOCS-TEST")
                    .build();

            HttpResponse<String> response = HttpClient.newBuilder()
                    .sslContext(contextLoader.getClientAuthSslContext(configuration.getMeshMailBoxClientCert(), configuration.getMeshMailBoxClientKey()))
                    .build()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            return getMessageIdFromMessage(response.body());
        } catch (Exception e) {
            log("Exception posting message on mailbox %s", e.getMessage());
            throw new HttpException("Exception encountered", e);
        }
    }

    public List<String> getMessageIds(String mailboxServiceUri) throws HttpException {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URL(mailboxServiceUri).toURI())
                    .GET()
                    .header("Authorization", getAuthToken())
                    .build();

            HttpResponse<String> response = HttpClient.newBuilder()
                    .sslContext(contextLoader.getClientAuthSslContext(configuration.getMeshMailBoxClientCert(), configuration.getMeshMailBoxClientKey()))
                    .build()
                    .send(request, HttpResponse.BodyHandlers.ofString());
            return getListOfMessagesOnMailbox(response.body());
        } catch (Exception e) {
            log("Exception getting message on mailbox %s", e.getMessage());
            throw new HttpException("Exception encountered", e);
        }
    }

    private String getAuthToken() throws Exception {
        return authTokenGenerator.getAuthorizationToken();
    }

    private String getMessageIdFromMessage(String responseBody) throws JSONException {
        JSONObject jsonObject = new JSONObject(responseBody);
        return String.valueOf(jsonObject.get("messageID"));
    }

    private List<String> getListOfMessagesOnMailbox(String responseBody) throws JSONException {
        JSONObject jsonObject = new JSONObject(responseBody);
        return getListFromJsonArray((JSONArray) jsonObject.get("messages"));
    }

    private List<String> getListFromJsonArray(JSONArray jsonArray) throws JSONException {
        ArrayList<String> list = new ArrayList<String>();
        if (jsonArray != null) {
            int len = jsonArray.length();
            for (int i = 0; i < len; i++) {
                list.add(jsonArray.get(i).toString());
            }
        }
        return list;
    }

    public void log(String messageBody, String messageValue) {
        System.out.println(String.format(messageBody, messageValue));
    }
}
