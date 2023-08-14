package uk.nhs.prm.e2etests.property;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.nhs.prm.e2etests.service.SsmService;

@Getter
@Component
public class EhrOutServiceProperties {
    @Value("${aws.configuration.serviceUrls.ehrOutService}")
    private String ehrOutServiceUrl;

    private final SsmService ssmService;

    @Autowired
    public EhrOutServiceProperties(SsmService ssmService) { this.ssmService = ssmService; }
}
