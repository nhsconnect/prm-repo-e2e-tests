package uk.nhs.prm.e2etests.configuration;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
public class Gp2gpConfiguration {
    @Value("${nhs.gp2gp.odsCodes.tppPtlInt}")
    private String tppPtlIntOdsCode;

    @Value("${nhs.gp2gp.odsCodes.emisPtlInt}")
    private String emisPtlIntOdsCode;

    @Value("${nhs.gp2gp.odsCodes.repoDev}")
    private String repoDevOdsCode;

    @Value("${nhs.gp2gp.odsCodes.repoTest}")
    private String repoTestOdsCode;
}
