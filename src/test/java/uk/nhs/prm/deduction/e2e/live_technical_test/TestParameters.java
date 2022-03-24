package uk.nhs.prm.deduction.e2e.live_technical_test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class TestParameters {
    public static void outputTestParameter(String name, String value) {
        File output = new File(name);
        System.out.println("Writing output test parameter to: " + output.getAbsolutePath());
        System.out.println(name + ": " + value);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(output))) {
            writer.write(value);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String fetchTestParameter(String name) {
        System.out.println("getting test parameter for " + name.toUpperCase());
        String envValue = System.getenv(name.toUpperCase());
        if (envValue != null) {
            System.out.println("got from environment");
            return envValue;
        }
        throw new RuntimeException("test parameter" +
                " for " + name + " not found in environment");
    }
}
