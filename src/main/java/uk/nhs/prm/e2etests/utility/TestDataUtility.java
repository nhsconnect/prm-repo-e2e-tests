package uk.nhs.prm.e2etests.utility;

import org.apache.commons.lang3.RandomStringUtils;
import lombok.extern.log4j.Log4j2;

import java.util.UUID;

@Log4j2
public final class TestDataUtility {
    private TestDataUtility() { }

    public static String randomOdsCode() {
        final String odsCode = RandomStringUtils
                .randomAlphanumeric(5, 5)
                .toUpperCase();
        log.info("Generated random ODS code: {}", odsCode);
        return odsCode;
    }

    public static String randomNhsNumber() {
        final String generatedNhsNumber = "9" + RandomStringUtils.randomNumeric(9);
        log.info("Generated random NHS number: {}", generatedNhsNumber);
        return generatedNhsNumber;
    }

    public static String randomNemsMessageId() {
        final String nemsEventMessageId = UUID.randomUUID().toString();
        log.info("Generated random NEMS Event Message ID: {}", nemsEventMessageId);
        return nemsEventMessageId;
    }

    public static String randomUppercaseUuidAsString() {
        return UUID.randomUUID().toString().toUpperCase();
    }
}
