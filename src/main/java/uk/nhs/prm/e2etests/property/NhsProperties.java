package uk.nhs.prm.e2etests.property;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.nhs.prm.e2etests.service.SsmService;

import java.util.Arrays;
import java.util.List;

@Log4j2
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
        String repoOdsCodeSsmParameter = ssmService.getSsmParameterValue(repoOdsCode);
        log.info("repositoryOdsCode: {}", repoOdsCodeSsmParameter);
        return repoOdsCodeSsmParameter;
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
