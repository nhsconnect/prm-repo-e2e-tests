package uk.nhs.prm.e2etests.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import uk.nhs.prm.e2etests.services.SsmService;

@Configuration
public class PdsAdaptorConfiguration extends AbstractSsmRetriever {
    @Value("${aws.configuration.ssm.parameters.pdsAdaptor.performanceApiKey}")
    private String performanceApiKey;

    @Value("${aws.configuration.ssm.parameters.pdsAdaptor.liveTestApiKey}")
    private String liveTestApiKey;

    @Value("${aws.configuration.ssm.parameters.pdsAdaptor.e2eTestApiKey}")
    private String e2eTestApiKey;

    @Autowired
    public PdsAdaptorConfiguration(SsmService ssmService) {
        super(ssmService);
    }

    public enum Type {
        PERFORMANCE_API_KEY,
        LIVE_TEST_API_KEY,
        E2E_TEST_API_KEY
    }

    public String getValue(Type type) {
        return switch (type) {
            case PERFORMANCE_API_KEY -> super.getAwsSsmParameterValue(this.performanceApiKey);
            case LIVE_TEST_API_KEY -> super.getAwsSsmParameterValue(this.liveTestApiKey);
            case E2E_TEST_API_KEY -> super.getAwsSsmParameterValue(this.e2eTestApiKey);
        };
    }
}