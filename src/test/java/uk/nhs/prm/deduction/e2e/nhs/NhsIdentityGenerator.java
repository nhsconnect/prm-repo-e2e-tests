package uk.nhs.prm.deduction.e2e.nhs;

import java.util.Random;
import java.util.UUID;

public class NhsIdentityGenerator {

    public static String generateRandomOdsCode() {
        int leftLimit = 48; // numeral '0'
        int rightLimit = 122; // letter 'z'
        int targetStringLength = 5;
        Random random = new Random();

        String generatedString = random.ints(leftLimit, rightLimit + 1)
                .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
                .limit(targetStringLength)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
        System.out.printf("Generating random ods code: %s%n", generatedString);
        return generatedString;
    }

    public static String randomNhsNumber() {
        return "9691234567" ;
    }

    public static String randomNemsMessageId() {
        String nemsMessageId = UUID.randomUUID().toString();
        System.out.println("Generated Nems Message ID " + nemsMessageId);
        return nemsMessageId;
    }
}
