package uk.nhs.prm.e2etests.configuration;

import org.springframework.context.annotation.Configuration;
import uk.nhs.prm.e2etests.services.SsmService;

@Configuration
public abstract class AbstractSsmRetriever {
    private final SsmService ssmService;

    public AbstractSsmRetriever(SsmService ssmService) {
        this.ssmService = ssmService;
    }

    protected String getAwsSsmParameterValue(String ssmParameterName) {
        return ssmService.getSsmParameterValue(ssmParameterName);
    }
}
