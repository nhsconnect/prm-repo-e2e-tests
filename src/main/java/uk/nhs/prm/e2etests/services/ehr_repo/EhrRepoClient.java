package uk.nhs.prm.e2etests.services.ehr_repo;

import com.google.gson.Gson;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.UUID;

@Component
public class EhrRepoClient {
    private final String apiKey;
    private final String rootUrl;
    private final RestTemplate restTemplate = new RestTemplate();

    public EhrRepoClient(String apiKey, String rootUrl) {
        this.apiKey = apiKey;
        this.rootUrl = rootUrl;
    }

    public boolean isPatientHealthRecordStatusComplete(String nhsNumber, String conversationId) {
        var ehrHealthRecordUrl = buildUrl(rootUrl, nhsNumber, conversationId);
        try {
            System.out.printf("Sending ehr status request to ehr repor: %s%n", ehrHealthRecordUrl);
            var exchange = restTemplate.exchange(ehrHealthRecordUrl, HttpMethod.GET, new HttpEntity<>(createHeaders(apiKey)), String.class);
            System.out.println("Ehr request successfully stored in ehr repo");
            return exchange.getStatusCode().is2xxSuccessful();
        } catch (HttpStatusCodeException e) {
            System.out.printf("Error retrieving ehr health record status from ehr repo. Status code: %s. Error: %s%n", e.getStatusCode(), e.getMessage());
            return false;
        }
    }

    public void createEhr(String nhsNumber) throws Exception {
        var conversationId = UUID.randomUUID();
        var messageId = UUID.randomUUID();
        var messageType = "ehrExtract";
        var fragmentMessageIds = Collections.EMPTY_LIST;

        var jsonPayloadString = new Gson().toJson(new StoreMessageRequestBody(messageId, conversationId, nhsNumber, messageType, fragmentMessageIds));

         restTemplate.exchange(new URL(rootUrl + "messages").toURI(), HttpMethod.POST,
                 new HttpEntity<>(jsonPayloadString, createHeaders(apiKey)), String.class);

    }

    public String getEhrResponse(String nhsNumber) throws MalformedURLException, URISyntaxException {
        return restTemplate.exchange(new URL(rootUrl + "patients/" + nhsNumber).toURI(), HttpMethod.GET,
                new HttpEntity<>(createHeaders(apiKey)), String.class).getStatusCode().toString();
    }

    private String buildUrl(String baseUrl, String nhsNumber, String conversationId) {
        return String.format("%spatients/%s/health-records/%s", baseUrl, nhsNumber, conversationId);
    }

    private HttpHeaders createHeaders(String apiKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("conversationId", "bob");
        return headers;
    }
}
