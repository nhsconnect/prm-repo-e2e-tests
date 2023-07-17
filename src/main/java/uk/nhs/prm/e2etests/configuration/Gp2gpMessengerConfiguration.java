package uk.nhs.prm.e2etests.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import uk.nhs.prm.e2etests.services.SsmService;
import lombok.Getter;

@Getter
@Configuration
public class Gp2gpMessengerConfiguration {
    private final SsmService ssmService;

    @Value("${nhs.services.gp2gp.odsCodes.tppPtlInt}")
    private String tppPtlIntOdsCode;

    @Value("${nhs.services.gp2gp.odsCodes.emisPtlInt}")
    private String emisPtlIntOdsCode;

    @Value("${nhs.services.gp2gp.odsCodes.repoDev}")
    private String repoDevOdsCode;

    @Value("${nhs.services.gp2gp.odsCodes.repoTest}")
    private String repoTestOdsCode;

    @Value("${aws.configuration.ssm.parameters.gp2gpMessenger.liveTestApiKey}")
    private String liveTestApiKey;

    @Autowired
    public Gp2gpMessengerConfiguration(SsmService ssmService) {
        this.ssmService = ssmService;
    }

    public enum Type {
        LIVE_TEST_API_KEY
    }

    public String getValue(Type type) {
        return switch (type) {
            case LIVE_TEST_API_KEY -> getAwsSsmParameterValue(this.liveTestApiKey);
        };
    }

    private String getAwsSsmParameterValue(String ssmParameterName) {
        return ssmService.getSsmParameterValue(ssmParameterName);
    }
}
