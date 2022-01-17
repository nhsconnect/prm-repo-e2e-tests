package uk.nhs.prm.deduction.e2e.pdsadaptor;

import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import uk.nhs.prm.deduction.e2e.TestConfiguration;

@Component
public class PdsAdaptorClient {

    private final String e2eAuthPassword;
    private final String pdsAdaptorUrl;

    public PdsAdaptorClient() {
        TestConfiguration config = new TestConfiguration();
        this.e2eAuthPassword  = config.getPdsAdaptorApiKey();
        this.pdsAdaptorUrl = buildUrl(config.getPdsAdaptorUrl(), config.getPdsAdaptorTestPatient());
    }

    public PdsAdaptorClient(String nhsNumber){
        TestConfiguration config = new TestConfiguration();
        this.e2eAuthPassword  = config.getPdsAdaptorApiKey();
        this.pdsAdaptorUrl = buildUrl(config.getPdsAdaptorUrl(), nhsNumber);
    }

    private final RestTemplate restTemplate = new RestTemplate();

    public PdsAdaptorResponse getSuspendedPatientStatus() {
        System.out.printf("Requesting patient suspended patient status from pds adaptor: %s%n", pdsAdaptorUrl);
        ResponseEntity<PdsAdaptorResponse> response =
            restTemplate.exchange(pdsAdaptorUrl, HttpMethod.GET, new HttpEntity<>(createHeaders()), PdsAdaptorResponse.class);
        System.out.printf("Response received from pds adaptor: %s%n", response.getBody());
        return response.getBody();
    }

    public PdsAdaptorResponse updateManagingOrganisation(String previousGp, String recordETag) {
        PdsAdaptorRequest request = new PdsAdaptorRequest(previousGp, recordETag);
        System.out.printf("Request to update patient : url - %s , request - %s%n", pdsAdaptorUrl, request);
        ResponseEntity<PdsAdaptorResponse> response =
            restTemplate.exchange(pdsAdaptorUrl, HttpMethod.PUT, new HttpEntity<>(request, createHeaders()), PdsAdaptorResponse.class);
        System.out.printf("Response received from pds adaptor update request: %s%n", response.getBody());
        return response.getBody();
    }

    private String buildUrl(String baseUrl, String nhsNumber) {
        return baseUrl + "suspended-patient-status/" + nhsNumber;
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth("e2e-test", e2eAuthPassword);
        headers.setContentType(MediaType.APPLICATION_JSON);

        return headers;
    }
}
