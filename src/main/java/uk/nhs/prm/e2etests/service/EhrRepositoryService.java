package uk.nhs.prm.e2etests.service;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import uk.nhs.prm.e2etests.exception.ServiceException;
import uk.nhs.prm.e2etests.model.MessageData;
import uk.nhs.prm.e2etests.property.EhrRepositoryProperties;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Log4j2
@Service
public class EhrRepositoryService {
    private final String ehrRepositoryApiKey;
    private final String ehrRepositoryUri;
    private final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    public EhrRepositoryService(
            EhrRepositoryProperties ehrRepositoryProperties
    ) {
        this.ehrRepositoryApiKey = ehrRepositoryProperties.getE2eTestApiKey();
        this.ehrRepositoryUri = ehrRepositoryProperties.getEhrRepositoryUrl();
    }

    public boolean isPatientHealthRecordStatusComplete(String nhsNumber, String conversationId) {
        String ehrHealthRecordUrl = buildUrl(ehrRepositoryUri, nhsNumber, conversationId);
        try {
            log.info("Sending EHR status request to EHR Repository: {}.", ehrHealthRecordUrl);
            ResponseEntity<String> exchange = restTemplate.exchange(ehrHealthRecordUrl, HttpMethod.GET, new HttpEntity<>(createHeaders(ehrRepositoryApiKey)), String.class);
            log.info("Ehr request successfully stored in EHR Repository.");
            return exchange.getStatusCode().is2xxSuccessful();
        } catch (HttpStatusCodeException exception) {
            throw new ServiceException(getClass().getName(), exception.getMessage());
        }
    }

    public void createEhr(String nhsNumber) throws Exception {
        UUID conversationId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        String messageType = "ehrExtract";
        List<UUID> fragmentMessageIds = Collections.emptyList();

         restTemplate.exchange(new URL(ehrRepositoryUri + "messages").toURI(), HttpMethod.POST,
                 new HttpEntity<>(new MessageData(
                         conversationId, messageId,
                         nhsNumber, messageType, fragmentMessageIds)
                         .getJsonString(), createHeaders(ehrRepositoryApiKey)), String.class);
    }

    public String getEhrResponse(String nhsNumber) throws MalformedURLException, URISyntaxException {
        return restTemplate.exchange(new URL(ehrRepositoryUri + "patients/" + nhsNumber).toURI(), HttpMethod.GET,
                new HttpEntity<>(createHeaders(ehrRepositoryApiKey)), String.class).getStatusCode().toString();
    }

    private String buildUrl(String baseUrl, String nhsNumber, String conversationId) {
        return String.format("%spatients/%s/health-records/%s", baseUrl, nhsNumber, conversationId);
    }

    private HttpHeaders createHeaders(String apiKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("conversationId", "bob");
        return headers;
    }
}
