package uk.nhs.prm.e2etests.configuration;

import lombok.AccessLevel;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.nhs.prm.e2etests.services.SsmService;

@Getter
@Component
public class PdsAdaptorPropertySource extends AbstractSsmRetriever {
    @Getter(AccessLevel.NONE)
    @Value("${aws.configuration.ssm.parameters.pdsAdaptor.performanceApiKey}")
    private String performanceApiKey;

    @Getter(AccessLevel.NONE)
    @Value("${aws.configuration.ssm.parameters.pdsAdaptor.liveTestApiKey}")
    private String liveTestApiKey;

    @Getter(AccessLevel.NONE)
    @Value("${aws.configuration.ssm.parameters.pdsAdaptor.e2eTestApiKey}")
    private String e2eTestApiKey;

    @Value("${aws.configuration.serviceUrls.pdsAdaptor}")
    private String pdsAdaptorUrl;

    @Autowired
    public PdsAdaptorPropertySource(SsmService ssmService) {
        super(ssmService);
    }

    public String getPerformanceApiKey() {
        return super.getAwsSsmParameterValue(this.performanceApiKey);
    }

    public String getLiveTestApiKey() {
        return super.getAwsSsmParameterValue(this.liveTestApiKey);
    }

    public String getE2eTestApiKey() {
        return super.getAwsSsmParameterValue(this.e2eTestApiKey);
    }
}