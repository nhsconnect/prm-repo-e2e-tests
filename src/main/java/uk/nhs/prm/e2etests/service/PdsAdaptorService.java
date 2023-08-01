package uk.nhs.prm.e2etests.service;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import uk.nhs.prm.e2etests.exception.InvalidPdsAdaptorUsernameException;
import uk.nhs.prm.e2etests.model.request.PdsAdaptorRequest;
import uk.nhs.prm.e2etests.model.response.PdsAdaptorResponse;
import uk.nhs.prm.e2etests.property.PdsAdaptorProperties;

@Log4j2
@ConditionalOnProperty(
        prefix = "test.pds",
        name = { "username" }
)
@Service
public class PdsAdaptorService {

    private final String apiKey;
    private final String patientRootUrl;
    private final String username;

    private final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    public PdsAdaptorService(PdsAdaptorProperties pdsAdaptorProperties, @Value("${test.pds.username}") String username) {
        this.patientRootUrl = pdsAdaptorProperties.getPdsAdaptorUrl();
        this.username = username;

        switch (username) {
            case "live-test" -> this.apiKey = pdsAdaptorProperties.getLiveTestApiKey();
            case "e2e-test" -> this.apiKey = pdsAdaptorProperties.getE2eTestApiKey();
            case "performance-test" -> this.apiKey = pdsAdaptorProperties.getPerformanceApiKey();
            default ->
                    throw new InvalidPdsAdaptorUsernameException(String.format("Received username: %s", username));
        }

    }

    public PdsAdaptorResponse getSuspendedPatientStatus(String nhsNumber) {
        var patientUrl = buildUrl(patientRootUrl, nhsNumber);
        log.info("Requesting patient status from pds adaptor: {}", patientRootUrl);
        ResponseEntity<PdsAdaptorResponse> response =
            restTemplate.exchange(patientUrl, HttpMethod.GET, new HttpEntity<>(createHeaders(username, apiKey)), PdsAdaptorResponse.class);
        log.info("Response received from pds adaptor: {}", response.getBody());
        return response.getBody();
    }

    public PdsAdaptorResponse updateManagingOrganisation(String nhsNumber, String previousGp, String recordETag) {
        var patientUrl = buildUrl(patientRootUrl, nhsNumber);
        PdsAdaptorRequest request = new PdsAdaptorRequest(previousGp, recordETag);
        log.info("Request to update patient : url - {} , request - {}", patientUrl, request);
        ResponseEntity<PdsAdaptorResponse> response =
            restTemplate.exchange(patientUrl, HttpMethod.PUT, new HttpEntity<>(request, createHeaders(username, apiKey)), PdsAdaptorResponse.class);
        log.info("Response received from pds adaptor update request: {}", response.getBody());
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
