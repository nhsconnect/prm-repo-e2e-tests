package uk.nhs.prm.deduction.e2e.utility;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Files {
    public static void outputTestData(String name, String value) {
        File output = new File(name);
        System.out.println("Writing output test data to: " + output.getAbsolutePath());
        System.out.println(name + ": " + value);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(output))) {
            writer.write(value);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
