package uk.nhs.prm.e2etests.utility;

import java.util.ArrayList;
import java.util.List;

public final class TestDataUtil {
    public static String generateRandomNhsNumber() {
        final double randonSevenDigitNumber = Math.floor(Math.random() * 9_000_000L) + 1_000_000L;
        return "969" + randonSevenDigitNumber;
    }

    public static List<String> perf(int numberOfNhsNumbers) {
        List<String> perfList = new ArrayList<String>();
        for (int i = 0; i < numberOfNhsNumbers ; i++) {
            var nhsNumber = generateRandomNhsNumber();
            perfList.add(nhsNumber);
        }
        return perfList;
    }
}
