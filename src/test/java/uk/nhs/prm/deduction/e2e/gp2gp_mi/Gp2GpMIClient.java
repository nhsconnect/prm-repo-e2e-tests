package uk.nhs.prm.deduction.e2e.gp2gp_mi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import uk.nhs.prm.deduction.e2e.gp2gp_mi.registration_started.Payload;
import uk.nhs.prm.deduction.e2e.gp2gp_mi.registration_started.Registration;
import uk.nhs.prm.deduction.e2e.gp2gp_mi.registration_started.RegistrationStartedRequest;
import uk.nhs.prm.deduction.e2e.gp2gp_mi.registration_started.RegistrationStartedResponse;

public class Gp2GpMIClient {

    private static final String apiKey = "3cbSGucYxh5qqCxrXXIzn1KPTqK0IEi5";
    private static final String patientRootUrl = "https://int.api.service.nhs.uk/gp-registrations-mi/";



    public <T> String updateManagingOrganisation(T request, String endpoint) throws JsonProcessingException {
        RestTemplate restTemplate = new RestTemplate();

        String json = new ObjectMapper().writer().writeValueAsString(request);
        System.out.printf("Request to update patient : url - %s , request - %s%n", patientRootUrl+endpoint, request);
        ResponseEntity<String> response =
                restTemplate.postForEntity(patientRootUrl+endpoint, new HttpEntity<>(json, createHeaders(apiKey)), String.class);
        System.out.printf("Response received from pds adaptor update request: %s%n", response.getBody());
        return response.getBody();
    }
    private static HttpHeaders createHeaders(String apiKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("apikey",apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
