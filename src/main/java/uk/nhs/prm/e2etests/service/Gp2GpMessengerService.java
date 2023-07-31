package uk.nhs.prm.e2etests.service;

import org.springframework.http.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import uk.nhs.prm.e2etests.annotation.Debt;
import uk.nhs.prm.e2etests.model.request.HealthRecordRequest;

@Debt(comment = "This needs to be annotated as a service and autowired in. There's a nested structure involving heavy" +
        "use of the 'new' keyword that we should aim to remove.", ticket = "PRMT-3532")
public class Gp2GpMessengerService {

    private final String apiKey;
    private final String rootUrl;
    private final RestTemplate restTemplate = new RestTemplate();

    public Gp2GpMessengerService(String apiKey, String rootUrl) {
        this.apiKey = apiKey;
        this.rootUrl = rootUrl;
    }

    public boolean isHealthRecordRequestSentSuccessful(String nhsNumber, String repoOdsCode, String repoAsid, String previousPractiseOdsCode, String conversationId) {
        var healthRecordRequestUrl = buildHealthRecordUrl(rootUrl, nhsNumber);
        try {
            var healthRecordRequest = new HealthRecordRequest(repoOdsCode, repoAsid, previousPractiseOdsCode, conversationId);
            System.out.printf("Sending health record request to gp2gp messenger: %s. Request body: %s%n", healthRecordRequestUrl, healthRecordRequest);
            var exchange = restTemplate.exchange(healthRecordRequestUrl, HttpMethod.POST, new HttpEntity<>(healthRecordRequest, createHeaders(apiKey)), String.class);
            System.out.println("Successfully sent ehr request to gp2gp messenger");
            return exchange.getStatusCode().is2xxSuccessful();
        } catch (HttpStatusCodeException e) {
            System.out.printf("Error sending ehr request from gp2gp-messenger. Status code: %s. Error: %s%n", e.getStatusCode(), e.getMessage());
            return false;
        }
    }

    public void getPdsRecordViaHl7v3(String nhsNumber) {
        var requestUrl = buildPdsUrl(rootUrl, nhsNumber);
        try {
            System.out.println("Sending pds hl7 request to gp2gp messenger");
            restTemplate.exchange(requestUrl, HttpMethod.GET, new HttpEntity<>(createHeaders(apiKey)), String.class);
            System.out.println("Successfully sent pds request to gp2gp messenger");
        } catch (HttpStatusCodeException e) {
            System.out.printf("Error sending pds request from gp2gp-messenger. Status code: %s. Error: %s%n", e.getStatusCode(), e.getMessage());
        }
    }

    private String buildHealthRecordUrl(String baseUrl, String nhsNumber) {
        return buildUrl(baseUrl, "health-record-requests/", nhsNumber);
    }


    private String buildPdsUrl(String baseUrl, String nhsNumber) {
        return buildUrl(baseUrl, "patient-demographics/", nhsNumber);
    }

    private String buildUrl(String baseUrl, String path, String nhsNumber) {
        return baseUrl + path + nhsNumber;
    }

    private HttpHeaders createHeaders(String apiKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
