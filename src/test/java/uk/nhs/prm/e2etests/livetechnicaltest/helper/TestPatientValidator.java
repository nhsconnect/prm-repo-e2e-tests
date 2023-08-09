package uk.nhs.prm.e2etests.livetechnicaltest.helper;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.nhs.prm.e2etests.property.NhsProperties;

import java.util.List;

@Log4j2
@Component
public class TestPatientValidator {
    private final List<String> safeListedNhsNumbers;
    private final String syntheticPatientPrefix;

    @Autowired
    public TestPatientValidator(NhsProperties nhsProperties) {
        safeListedNhsNumbers = nhsProperties.getSafeListedPatientList();
        syntheticPatientPrefix = nhsProperties.getSyntheticPatientPrefix();
    }

    public boolean isIncludedInTheTest(String nhsNumber) {
        log.info("Checking if nhs number is safe listed or synthetic");
        return (safeListedNhsNumbers.contains(nhsNumber) || nhsNumber.startsWith(syntheticPatientPrefix));
    }
}
