package uk.nhs.prm.deduction.e2e.services.gp2gp_messenger;

import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

@Component
public class Gp2GpMessengerClient {

    private final String apiKey;
    private final String rootUrl;
    private final RestTemplate restTemplate = new RestTemplate();

    public Gp2GpMessengerClient(String apiKey, String rootUrl) {
        this.apiKey = apiKey;
        this.rootUrl = rootUrl;
    }

    public boolean isHealthRecordRequestSentSuccessful(String nhsNumber, String repoOdsCode, String repoAsid, String previousPractiseOdsCode, String conversationId) {
        var healthRecordRequestUrl = buildUrl(rootUrl, nhsNumber);
        try {
            System.out.printf("Sending health record request to gp2gp messenger: %s%n", healthRecordRequestUrl);
            var healthRecordRequest = new HealthRecordRequest(repoOdsCode, repoAsid, previousPractiseOdsCode, conversationId);
            var exchange = restTemplate.exchange(healthRecordRequestUrl, HttpMethod.POST, new HttpEntity<>(healthRecordRequest, createHeaders(apiKey)), String.class);
            System.out.println("Successfully sent ehr request to gp2gp messenger");
            return exchange.getStatusCode().is2xxSuccessful();
        } catch (HttpStatusCodeException e) {
            System.out.printf("Error sending ehr request from gp2gp-messenger. Status code: %s. Error: %s", e.getStatusCode(), e.getMessage());
            return false;
        }
    }

    private String buildUrl(String baseUrl, String nhsNumber) {
        return baseUrl + "health-record-requests/" + nhsNumber;
    }

    private HttpHeaders createHeaders(String apiKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
