package uk.nhs.prm.e2etests.utility;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.RandomStringUtils;
import uk.nhs.prm.e2etests.annotation.Debt;

import java.util.UUID;

@Log4j2
public final class NhsIdentityUtility {
    private NhsIdentityUtility() { }

    public static String randomOdsCode() {
        final String generatedOdsCode = RandomStringUtils.randomAlphanumeric(5, 5);
        log.info("Generated random ODS code: {}", generatedOdsCode);
        return generatedOdsCode;
    }

    @Debt(comment = "Misleading. The class states 'NhsIdentityGenerator' but this is a number picked at random.")
    public static String randomNhsNumber() {
        return "9691234567" ;
    }

    public static String randomNemsMessageId() {
        final String nemsEventMessageId = UUID.randomUUID().toString();
        log.info("Generated random NEMS Event Message ID: {}", nemsEventMessageId);
        return nemsEventMessageId;
    }
}
