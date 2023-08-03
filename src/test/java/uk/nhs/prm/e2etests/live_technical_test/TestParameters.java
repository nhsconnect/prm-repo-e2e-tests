package uk.nhs.prm.e2etests.live_technical_test;

import lombok.extern.log4j.Log4j2;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

@Log4j2
public class TestParameters {
    public static void outputTestParameter(String name, String value) {
        File output = new File(name);
        log.info("Writing output test parameter to: {}.", output.getAbsolutePath());
        log.info("{}:{}", name, value);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(output))) {
            writer.write(value);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String fetchTestParameter(String name) {
        log.info("Getting test parameter for: {}.", name.toUpperCase());
        String envValue = System.getenv(name.toUpperCase());
        if (envValue != null) {
            log.info("Found {} from test environment.", name.toUpperCase());
            return envValue;
        }
        throw new RuntimeException("test parameter" +
                " for " + name + " not found in environment");
    }
}
