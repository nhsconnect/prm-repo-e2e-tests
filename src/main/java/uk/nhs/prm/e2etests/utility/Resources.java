package uk.nhs.prm.e2etests.utility;

import java.io.*;

// TODO PRMT-3574 - MAKE THIS A TRUE UTILITY OR REMOVE COMPLETELY IN FAVOR OF
// RESOURCE LOADER.
public class Resources {

    public static String readTestResourceFile(String filename) {
        try {
            File file = new File(String.format("src/test/resources/%s", filename));
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            StringBuilder sb = new StringBuilder();

            while ((line = br.readLine()) != null) {
                sb.append(line.trim());
            }
            return sb.toString();
        }
        catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public static String readTestResourceFileFromEhrDirectory(String filename) {
        return readTestResourceFile("ehr/" + filename);
    }

    public static String readTestResourceFileFromNemsEventTemplatesDirectory(String filename) {
        return readTestResourceFile("nems-event-templates/" + filename);
    }

}
