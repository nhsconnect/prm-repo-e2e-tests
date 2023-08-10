package uk.nhs.prm.e2etests.property;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.nhs.prm.e2etests.service.SsmService;

@Component
public class PdsAdaptorProperties {
    @Value("${aws.configuration.ssm.parameters.pdsAdaptor.performanceApiKey}")
    private String performanceApiKey;

    @Value("${aws.configuration.ssm.parameters.pdsAdaptor.liveTestApiKey}")
    private String liveTestApiKey;

    @Value("${aws.configuration.ssm.parameters.pdsAdaptor.e2eTestApiKey}")
    private String e2eTestApiKey;

    @Getter
    @Value("${aws.configuration.serviceUrls.pdsAdaptor}")
    private String pdsAdaptorUrl;

    private final SsmService ssmService;
    @Autowired
    public PdsAdaptorProperties(SsmService ssmService) {
        this.ssmService = ssmService;
    }

    public String getPerformanceApiKey() {
        return ssmService.getSsmParameterValue(this.performanceApiKey);
    }

    public String getLiveTestApiKey() {
        return ssmService.getSsmParameterValue(this.liveTestApiKey);
    }

    public String getE2eTestApiKey() {
        return ssmService.getSsmParameterValue(this.e2eTestApiKey);
    }

}