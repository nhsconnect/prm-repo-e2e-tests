package uk.nhs.prm.e2etests.service;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import uk.nhs.prm.e2etests.exception.ServiceException;
import uk.nhs.prm.e2etests.property.EhrOutServiceProperties;
import uk.nhs.prm.e2etests.property.EhrRepositoryProperties;
import uk.nhs.prm.e2etests.property.Gp2gpMessengerProperties;

import java.util.Map;

@Log4j2
@Service
public class HealthCheckService {
    private final RestTemplate restTemplate = new RestTemplate();

    private final Map<String, String> listOfServices;

    @Autowired
    public HealthCheckService(
            Gp2gpMessengerProperties gp2gpMessengerProperties,
            EhrRepositoryProperties ehrRepositoryProperties,
            EhrOutServiceProperties ehrOutServiceProperties
    ) {
        // Ehr Transfer Service doesn't seem to have a health check endpoint for now, so it is not included here.
        this.listOfServices = Map.of(
                "Gp2gp Messenger", gp2gpMessengerProperties.getGp2gpMessengerUrl(),
                "Ehr Repository", ehrRepositoryProperties.getEhrRepositoryUrl(),
                "Ehr Out Service", ehrOutServiceProperties.getEhrOutServiceUrl()
        );
    }

    public boolean healthCheckAllPassing() {
        return this.listOfServices.entrySet().stream().allMatch(
                entry -> runHealthCheck(entry.getKey(), entry.getValue())
        );
    }

    private boolean runHealthCheck(String nameOfService, String baseUrl) {
        log.info("Running health check for service: {}", nameOfService);
        String ehrRepoHealthCheckUrl = String.format("%s/health", baseUrl);
        try {
            ResponseEntity<String> exchange = restTemplate.exchange(ehrRepoHealthCheckUrl,
                    HttpMethod.GET, new HttpEntity<String>(createHeaders()), String.class);
            return exchange.getStatusCode().is2xxSuccessful();
        } catch (HttpStatusCodeException exception) {
            throw new ServiceException(getClass().getName(), exception.getMessage());
        }
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
