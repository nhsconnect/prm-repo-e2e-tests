package uk.nhs.prm.e2etests.property;

import lombok.AccessLevel;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.nhs.prm.e2etests.service.SsmService;

@Getter
@Component
public class Gp2gpMessengerProperties {
    @Value("${nhs.services.gp2gp.odsCodes.tppPtlInt}")
    private String tppPtlIntOdsCode;

    @Value("${nhs.services.gp2gp.odsCodes.emisPtlInt}")
    private String emisPtlIntOdsCode;

    @Getter(AccessLevel.NONE)
    @Value("${aws.configuration.ssm.parameters.gp2gpMessenger.liveTestApiKey}")
    private String liveTestApiKey;

    @Value("${aws.configuration.serviceUrls.gp2GpMessenger}")
    private String gp2gpMessengerUrl;

    private final SsmService ssmService;

    @Autowired
    public Gp2gpMessengerProperties(SsmService ssmService) {
        this.ssmService = ssmService;
    }

    public String getLiveTestApiKey() {
        return this.ssmService.getSsmParameterValue(this.liveTestApiKey);
    }
}
