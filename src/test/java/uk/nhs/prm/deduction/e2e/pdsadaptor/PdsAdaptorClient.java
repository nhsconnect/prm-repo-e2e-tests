package uk.nhs.prm.deduction.e2e.pdsadaptor;

import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import uk.nhs.prm.deduction.e2e.TestConfiguration;

@Component
public class PdsAdaptorClient {

    private final String e2eAuthPassword;
    private final String patientRootUrl;

    public PdsAdaptorClient() {
        TestConfiguration config = new TestConfiguration();
        this.e2eAuthPassword  = config.getPdsAdaptorApiKey();
        this.patientRootUrl = config.getPdsAdaptorUrl();
    }

    private final RestTemplate restTemplate = new RestTemplate();

    public PdsAdaptorResponse getSuspendedPatientStatus(String nhsNumber) {
        var patientUrl = buildUrl(patientRootUrl, nhsNumber);
        System.out.printf("Requesting patient suspended patient status from pds adaptor: %s%n", patientRootUrl);
        ResponseEntity<PdsAdaptorResponse> response =
            restTemplate.exchange(patientUrl, HttpMethod.GET, new HttpEntity<>(createHeaders()), PdsAdaptorResponse.class);
        System.out.printf("Response received from pds adaptor: %s%n", response.getBody());
        return response.getBody();
    }

    public PdsAdaptorResponse updateManagingOrganisation(String nhsNumber, String previousGp, String recordETag) {
        var patientUrl = buildUrl(patientRootUrl, nhsNumber);
        PdsAdaptorRequest request = new PdsAdaptorRequest(previousGp, recordETag);
        System.out.printf("Request to update patient : url - %s , request - %s%n", patientUrl, request);
        ResponseEntity<PdsAdaptorResponse> response =
            restTemplate.exchange(patientUrl, HttpMethod.PUT, new HttpEntity<>(request, createHeaders()), PdsAdaptorResponse.class);
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
