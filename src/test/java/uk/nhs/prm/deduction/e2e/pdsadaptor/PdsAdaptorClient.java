package uk.nhs.prm.deduction.e2e.pdsadaptor;

import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

public class PdsAdaptorClient {

    private final String apiKey;
    private final String patientRootUrl;
    private final String username;
    private final RestTemplate restTemplate = new RestTemplate();

    public PdsAdaptorClient(String username, String apiKey, String pdsAdaptorUrl) {
        this.username = username;
        this.apiKey = apiKey;
        this.patientRootUrl = pdsAdaptorUrl;
    }

    public PdsAdaptorResponse getSuspendedPatientStatus(String nhsNumber) {
        var patientUrl = buildUrl(patientRootUrl, nhsNumber);
        System.out.printf("Requesting patient status from pds adaptor: %s%n", patientRootUrl);
        ResponseEntity<PdsAdaptorResponse> response =
            restTemplate.exchange(patientUrl, HttpMethod.GET, new HttpEntity<>(createHeaders(username, apiKey)), PdsAdaptorResponse.class);
        System.out.printf("Response received from pds adaptor: %s%n", response.getBody());
        return response.getBody();
    }

    public PdsAdaptorResponse updateManagingOrganisation(String nhsNumber, String previousGp, String recordETag) {
        var patientUrl = buildUrl(patientRootUrl, nhsNumber);
        PdsAdaptorRequest request = new PdsAdaptorRequest(previousGp, recordETag);
        System.out.printf("Request to update patient : url - %s , request - %s%n", patientUrl, request);
        ResponseEntity<PdsAdaptorResponse> response =
            restTemplate.exchange(patientUrl, HttpMethod.PUT, new HttpEntity<>(request, createHeaders(username, apiKey)), PdsAdaptorResponse.class);
        System.out.printf("Response received from pds adaptor update request: %s%n", response.getBody());
        return response.getBody();
    }

    private String buildUrl(String baseUrl, String nhsNumber) {
        return baseUrl + "suspended-patient-status/" + nhsNumber;
    }

    private HttpHeaders createHeaders(String username, String apiKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(username, apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
