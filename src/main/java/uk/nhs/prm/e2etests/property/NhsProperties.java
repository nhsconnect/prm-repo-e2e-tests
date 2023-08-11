package uk.nhs.prm.e2etests.property;

import com.amazonaws.services.dynamodbv2.xspec.SS;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.nhs.prm.e2etests.service.SsmService;

import java.util.Arrays;
import java.util.List;

@Getter
@Component
public class NhsProperties {
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

    private final SsmService ssmService;

    @Autowired
    public NhsProperties(SsmService ssmService) {
        this.ssmService = ssmService;
    }

    public String getRepoOdsCode() {
        return ssmService.getSsmParameterValue(repoOdsCode);
    }

    public List<String> getSafeListedPatientList() {
        String rawSafeListedPatientListStringFromSsm = ssmService.getSsmParameterValue(safeListedPatients);
        return Arrays.asList(rawSafeListedPatientListStringFromSsm.split(","));
    }

    public String getSyntheticPatientPrefix() {
        return nhsEnvironment.equals("prod")
                ? syntheticPatientNhsNumberPrefixProd
                : syntheticPatientNhsNumberPrefixNonProd;
    }
}
