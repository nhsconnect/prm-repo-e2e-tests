package uk.nhs.prm.e2etests.service;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import uk.nhs.prm.e2etests.annotation.Debt;
import uk.nhs.prm.e2etests.model.request.HealthRecordRequest;
import uk.nhs.prm.e2etests.property.Gp2gpMessengerProperties;

@Log4j2
@Service
public class Gp2GpMessengerService {

    private final String apiKey;
    private final String rootUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    public Gp2GpMessengerService(Gp2gpMessengerProperties gp2gpMessengerProperties) {
        this.apiKey = gp2gpMessengerProperties.getLiveTestApiKey();
        this.rootUrl = gp2gpMessengerProperties.getGp2gpMessengerUrl();
    }

    public boolean isHealthRecordRequestSentSuccessful(String nhsNumber, String repoOdsCode, String repoAsid, String previousPractiseOdsCode, String conversationId) {
        String healthRecordRequestUrl = buildHealthRecordUrl(rootUrl, nhsNumber);
        try {
            HealthRecordRequest healthRecordRequest = new HealthRecordRequest(repoOdsCode, repoAsid, previousPractiseOdsCode, conversationId);
            log.info("Sending health record request to GP2GP Messenger {} with request body: {}", healthRecordRequestUrl, healthRecordRequest);
            ResponseEntity<String> exchange = restTemplate.exchange(healthRecordRequestUrl, HttpMethod.POST, new HttpEntity<>(healthRecordRequest, createHeaders(apiKey)), String.class);
            log.info("Successfully sent EHR request to GP2GP Messenger.");
            return exchange.getStatusCode().is2xxSuccessful();
        } catch (HttpStatusCodeException exception) {
            log.info("An error occurred while sending EHR request to GP2GP Messenger with status code {}, details: {}.",
                    exception.getStatusCode(),
                    exception.getMessage());
            return false;
        }
    }

    public void getPdsRecordViaHl7v3(String nhsNumber) {
        String requestUrl = buildPdsUrl(rootUrl, nhsNumber);
        try {
            log.info("Sending PDS HL7v3 request to GP2GP Messenger.");
            restTemplate.exchange(requestUrl, HttpMethod.GET, new HttpEntity<>(createHeaders(apiKey)), String.class);
            log.info("Successfully sent PDS HL7v3 request to GP2GP Messenger.");
        } catch (HttpStatusCodeException exception) {
            log.info("An error occurred while sending PDS request from GP2GP Messenger with status code {}, details: {}.",
                    exception.getStatusCode(),
                    exception.getMessage());
        }
    }

    private String buildHealthRecordUrl(String baseUrl, String nhsNumber) {
        return buildUrl(baseUrl, "health-record-requests/", nhsNumber);
    }

    private String buildPdsUrl(String baseUrl, String nhsNumber) {
        return buildUrl(baseUrl, "patient-demographics/", nhsNumber);
    }

    private String buildUrl(String baseUrl, String path, String nhsNumber) {
        return baseUrl + path + nhsNumber;
    }

    private HttpHeaders createHeaders(String apiKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
