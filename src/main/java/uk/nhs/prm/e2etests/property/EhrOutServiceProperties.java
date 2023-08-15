package uk.nhs.prm.e2etests.property;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class EhrOutServiceProperties {
    @Value("${aws.configuration.serviceUrls.ehrOutService}")
    private String ehrOutServiceUrl;

    public String getEhrOutServiceUrl() {
        return ehrOutServiceUrl;
    }
}
