package uk.nhs.prm.deduction.e2e.utility;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.regex.Pattern;

public final class TestUtils {
    private static final Logger LOGGER = LogManager.getLogger(TestUtils.class);
    private static final Pattern UUID_REGEX_PATTERN =
            Pattern.compile("^[{]?[0-9a-fA-F]{8}-([0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12}[}]?$");

    public static boolean isValidUUID(String uuid) {
        try {
            return UUID_REGEX_PATTERN.matcher(uuid).matches();
        } catch (IllegalArgumentException exception) {
            LOGGER.error("Exception occurred while testing UUID validity: {}", exception.getMessage());
            return false;
        }
    }
}
