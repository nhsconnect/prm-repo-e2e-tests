package uk.nhs.prm.deduction.e2e.utility;

import org.springframework.http.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;


public class HealthCheck {
    private static final RestTemplate restTemplate = new RestTemplate();
    public static boolean isHealthCheckPassing(String rootUrl) {
        String healthCheckUrl = rootUrl + "health";
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            ResponseEntity<String> exchange = restTemplate.exchange(healthCheckUrl, HttpMethod.GET, new HttpEntity<String>(headers), String.class);
            System.out.printf("checking service health status of rootUrl: %s" , rootUrl);
            return exchange.getStatusCode().is2xxSuccessful();
        } catch (HttpStatusCodeException e) {
            System.out.printf("Error retrieving health check status from %s. Status code: %s. Error: %s%n", rootUrl, e.getStatusCode(), e.getMessage());
            return false;
        }
    }
}
