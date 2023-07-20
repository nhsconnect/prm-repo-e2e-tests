package uk.nhs.prm.e2etests.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.nhs.prm.e2etests.services.SsmService;

import java.util.Arrays;
import java.util.List;

@Component
public class NhsPropertySource extends AbstractSsmRetriever {
    @Value("${nhs.environment:#{'dev'}")
    private String nhsEnvironment;

    @Value("${aws.configuration.ssm.parameters.ehrRepository.odsCode}")
    private String repoOdsCode;

    @Value("${aws.configuration.ssm.parameters.safeListedPatients}")
    private String safeListedPatients;

    @Value("${nhs.syntheticPatientNhsNumberPrefix.prod}")
    private String syntheticPatientNhsNumberPrefixProd;

    @Value("${nhs.syntheticPatientNhsNumberPrefix.nonProd}")
    private String syntheticPatientNhsNumberPrefixNonProd;

    public String getNhsEnvironment() {
        return nhsEnvironment;
    }

    @Autowired
    public NhsPropertySource(SsmService ssmService) {
        super(ssmService);
    }

    public String getRepoOdsCode() {
        return super.getAwsSsmParameterValue(repoOdsCode);
    }

    public List<String> getSafeListedPatientList() {
        return Arrays.asList(safeListedPatients.split(","));
    }

    public String getSyntheticPatientPrefix() {
        return nhsEnvironment.equals("prod")
                ? syntheticPatientNhsNumberPrefixProd
                : syntheticPatientNhsNumberPrefixNonProd;
    }
}
