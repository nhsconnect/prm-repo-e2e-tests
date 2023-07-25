package uk.nhs.prm.e2etests.utility;

import java.io.*;

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
        try {
            File file = new File(String.format("src/test/resources/ehr/%s", filename));
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

    public static String readTestResourceFileFromNemsEventTemplatesDirectory(String filename) {
        try {
            File file = new File(String.format("src/test/resources/nems-event-templates/%s", filename));
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
}
