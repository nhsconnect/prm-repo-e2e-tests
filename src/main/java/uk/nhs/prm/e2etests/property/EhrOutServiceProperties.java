package uk.nhs.prm.e2etests.property;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @deprecated PRMT-4670 old Transfer Tracker DB logic - to be removed
 */
@Deprecated(since="2.0.0", forRemoval = true)
@Component
public class EhrOutServiceProperties {
    @Value("${aws.configuration.serviceUrls.ehrOutService}")
    private String ehrOutServiceUrl;

    public String getEhrOutServiceUrl() {
        return ehrOutServiceUrl;
    }
}
