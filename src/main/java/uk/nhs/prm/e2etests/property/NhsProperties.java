package uk.nhs.prm.e2etests.property;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.nhs.prm.e2etests.service.SsmService;

import java.util.Arrays;
import java.util.List;

@Component
public class NhsProperties extends AbstractSsmRetriever {
    @Value("${nhs.environment}")
    private String nhsEnvironment;

    @Value("${aws.configuration.ssm.parameters.ehrRepository.odsCode}")
    private String repoOdsCode;

    @Value("${aws.configuration.ssm.parameters.safeListedPatients}")
    private String safeListedPatients;

    @Value("${nhs.syntheticPatient.nhsNumberPrefix.prod}")
    private String syntheticPatientNhsNumberPrefixProd;

    @Value("${nhs.syntheticPatient.nhsNumberPrefix.nonProd}")
    private String syntheticPatientNhsNumberPrefixNonProd;

    public String getNhsEnvironment() {
        return nhsEnvironment;
    }

    @Autowired
    public NhsProperties(SsmService ssmService) {
        super(ssmService);
    }

    public String getRepoOdsCode() {
        return super.getAwsSsmParameterValue(repoOdsCode);
    }

    public List<String> getSafeListedPatientList() {
        String rawSafeListedPatientListStringFromSsm = super.getAwsSsmParameterValue(safeListedPatients);
        return Arrays.asList(rawSafeListedPatientListStringFromSsm.split(","));
    }

    public String getSyntheticPatientPrefix() {
        return nhsEnvironment.equals("prod")
                ? syntheticPatientNhsNumberPrefixProd
                : syntheticPatientNhsNumberPrefixNonProd;
    }
}
