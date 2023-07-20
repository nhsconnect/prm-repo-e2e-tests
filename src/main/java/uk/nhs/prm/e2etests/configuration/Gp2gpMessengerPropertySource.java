package uk.nhs.prm.e2etests.configuration;

import lombok.AccessLevel;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.nhs.prm.e2etests.services.SsmService;

@Getter
@Component
public class Gp2gpMessengerPropertySource extends AbstractSsmRetriever {
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
    public Gp2gpMessengerPropertySource(SsmService ssmService) {
        super(ssmService);
    }

    public String getLiveTestApiKey() {
        return super.getAwsSsmParameterValue(this.liveTestApiKey);
    }
}
