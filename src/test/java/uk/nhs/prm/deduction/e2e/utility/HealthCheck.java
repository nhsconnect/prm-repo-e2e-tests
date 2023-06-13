package uk.nhs.prm.deduction.e2e.utility;

import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.is;


public class HealthCheck {
    private static final RestTemplate restTemplate = new RestTemplate();
    public static boolean isHealthCheckPassing(String rootUrl) {
        String healthCheckUrl = rootUrl + "health";
        System.out.printf("checking service health status of rootUrl: %s" , rootUrl);

        try {
            return await().atMost(5, TimeUnit.SECONDS)
                    .with()
                    .pollInterval(200, TimeUnit.MILLISECONDS)
                    .until(() -> getRequest(healthCheckUrl).getStatusCode().is2xxSuccessful(), is(true));
        } catch (Exception e) {
            System.out.printf("Error retrieving health check status from %s. Error: %s%n", rootUrl, e.getMessage());
            return false;
        }
    }

    private static ResponseEntity<String> getRequest(String url) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<String>(headers), String.class);
    }
}
