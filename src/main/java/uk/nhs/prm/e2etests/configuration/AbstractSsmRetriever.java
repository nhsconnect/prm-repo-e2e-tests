package uk.nhs.prm.e2etests.configuration;

import uk.nhs.prm.e2etests.services.SsmService;

public abstract class AbstractSsmRetriever {
    private final SsmService ssmService;

    protected AbstractSsmRetriever(SsmService ssmService) {
        this.ssmService = ssmService;
    }

    protected String getAwsSsmParameterValue(String ssmParameterName) {
        return ssmService.getSsmParameterValue(ssmParameterName);
    }
}
