package uk.nhs.prm.e2etests.property;

import uk.nhs.prm.e2etests.service.SsmService;

public abstract class AbstractSsmRetriever {
    private final SsmService ssmService;

    protected AbstractSsmRetriever(SsmService ssmService) {
        this.ssmService = ssmService;
    }

    protected String getAwsSsmParameterValue(String ssmParameterName) {
        return ssmService.getSsmParameterValue(ssmParameterName);
    }
}
