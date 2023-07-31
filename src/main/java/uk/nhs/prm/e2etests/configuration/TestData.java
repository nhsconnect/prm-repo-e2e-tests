package uk.nhs.prm.e2etests.configuration;

import java.util.List;
import java.util.stream.Stream;

import static java.util.Arrays.asList;

public class TestData {
    public static List<String> dev() {
        return asList(
                "9693797426",
                "9693797477",
                "9693797361",
                "9693797353",
                "9693797345",
                "9693797337",
                "9693797329",
                "9693797310",
                "9693797469",
                "9693797434",
                "9693797167",
                "9693797159",
                "9693796012",
                "9693796004",
                "9693796020",
                "9693796055",
                "9693796063",
                "9693796071",
                "9693796098",
                "9693796101",
                "9693796128",
                "9693796136",
                "9693796144",
                "9693796152",
                "9693796160",
                "9693796179",
                "9693796187",
                "9693796195",
                "9693796209",
                "9693796217",
                "9693796225",
                "9693796233",
                "9693796241",
                "9693796268",
                "9693796276");
    }

    public static List<String> preProd() {
        return asList(
                "9693642422",
                "9693642430",
                "9693642449",
                "9693642457",
                "9693642465",
                "9693642473",
                "9693642481",
                "9693642503",
                "9693642511",
                "9693642538");
    }

    public static List<String> perf(int numberOfNhsNumbers) {
        return Stream
                .generate(TestData::generateRandomNhsNumber)
                .limit(numberOfNhsNumbers)
                .toList();
    }

    public static String generateRandomNhsNumber() {
        long randomSevenDigitNumber = (long) Math.floor(Math.random() * 9_000_000L) + 1_000_000L;
        return "969" + randomSevenDigitNumber;
    }
}
