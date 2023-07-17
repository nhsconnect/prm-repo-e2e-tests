package uk.nhs.prm.e2etests.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import uk.nhs.prm.e2etests.services.SsmService;

@Configuration
public class EhrRepositoryRetriever extends AbstractSsmRetriever {
    @Value("${aws.configuration.ssm.parameters.ehrRepository.liveTestApiKey}")
    private String liveTestApiKey;

    @Value("${aws.configuration.ssm.parameters.ehrRepository.e2eTestApiKey}")
    private String e2eTestApiKey;

    @Autowired
    public EhrRepositoryRetriever(SsmService ssmService) {
        super(ssmService);
    }

    public enum Type {
        LIVE_TEST_API_KEY,
        E2E_TEST_API_KEY
    }

    public String getValue(Type type) {
        return switch (type) {
            case LIVE_TEST_API_KEY -> super.getAwsSsmParameterValue(this.liveTestApiKey);
            case E2E_TEST_API_KEY -> super.getAwsSsmParameterValue(this.e2eTestApiKey);
        };
    }
}