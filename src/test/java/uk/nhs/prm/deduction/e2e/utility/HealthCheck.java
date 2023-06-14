package uk.nhs.prm.deduction.e2e.utility;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.is;


public class HealthCheck {
    private static final RestTemplate restTemplate = new RestTemplate();
    private static final Logger LOGGER = LogManager.getLogger(TestUtils.class);

    public static boolean isHealthCheckPassing(String rootUrl) {
        String healthCheckUrl = rootUrl + "health";
        LOGGER.info("checking service health status of rootUrl: {}" , rootUrl);

        try {
            return await().atMost(5, TimeUnit.SECONDS)
                    .with()
                    .pollInterval(200, TimeUnit.MILLISECONDS)
                    .until(() -> getRequest(healthCheckUrl).getStatusCode().is2xxSuccessful(), is(true));
        } catch (Exception e) {
            LOGGER.info("Error retrieving health check status from {}. Error: {}", rootUrl, e.getMessage());
            return false;
        }
    }

    private static ResponseEntity<String> getRequest(String url) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<String>(headers), String.class);
    }
}
