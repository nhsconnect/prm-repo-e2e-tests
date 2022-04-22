package uk.nhs.prm.deduction.e2e.services.ehr_repo;

import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

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

    private String buildUrl(String baseUrl, String nhsNumber, String conversationId) {
        return String.format("%spatients/%s/health-records/%s", baseUrl, nhsNumber, conversationId);
    }

    private HttpHeaders createHeaders(String apiKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
