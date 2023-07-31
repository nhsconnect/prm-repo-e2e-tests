package uk.nhs.prm.e2etests.utility;

import lombok.extern.log4j.Log4j2;
import uk.nhs.prm.e2etests.annotation.Debt;

import java.util.Random;
import java.util.UUID;

@Log4j2
public final class NhsIdentityGenerator {
    private NhsIdentityGenerator() { }

    public static String randomOdsCode() {
        int leftLimit = 48; // numeral '0'
        int rightLimit = 122; // letter 'z'
        int targetStringLength = 5;
        Random random = new Random();

        return random.ints(leftLimit, rightLimit + 1)
                .filter(integer -> (integer <= 57 || integer >= 65) && (integer <= 90 || integer >= 97))
                .limit(targetStringLength)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    @Debt(comment = "Misleading. The class states 'NhsIdentityGenerator' but this is a number picked at random.")
    public static String randomNhsNumber() {
        return "9691234567" ;
    }

    public static String randomNemsMessageId() {
        return randomNemsMessageId(true);
    }

    public static String randomNemsMessageId(boolean shouldLog) {
        return UUID.randomUUID().toString();
    }
}
