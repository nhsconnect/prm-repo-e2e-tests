package uk.nhs.prm.e2etests.live_technical_test.helpers;

import org.springframework.beans.factory.annotation.Autowired;
import uk.nhs.prm.e2etests.configuration.NhsPropertySource;
import org.springframework.stereotype.Component;
import lombok.extern.log4j.Log4j2;

import java.util.List;

@Log4j2
@Component
public class TestPatientValidator {

    public boolean isIncludedInTheTest(String nhsNumber, String safeListString, String syntheticPrefix) {
        System.out.println("Checking if nhs number is safe listed or synthetic");
        if (safeListContains(nhsNumber, safeListString) || nhsNumber.startsWith(syntheticPrefix)) {
            return true;
        }
        return false;
    }

    private boolean safeListContains(String nhsNumber, String safeListString) {
        if (safeListString != null && safeListString.split(",").length > 0) {
            System.out.println(safeListString.contains(nhsNumber) ? "Patient is safe listed" : "Patient is not present in safe list");
            return safeListString.contains(nhsNumber);
        }
        return false;
    }
}
