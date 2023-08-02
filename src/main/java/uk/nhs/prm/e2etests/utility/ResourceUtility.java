package uk.nhs.prm.e2etests.utility;

import lombok.Getter;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public final class ResourceUtility {
    private static final ResourceLoader resourceLoader;

    static {
        resourceLoader = new FileSystemResourceLoader();
    }

    private ResourceUtility() { }

    static String readTestResourceFile(Directory directory, String filename) {
        try {
            return resourceLoader
                    .getResource(String.format("classpath:%s/%s", directory.getDirectory(), filename))
                    .getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new ResourceAccessException(exception.getMessage());
        }
    }

    public static String readTestResourceFileFromEhrDirectory(String filename) {
        return readTestResourceFile(Directory.EHR_DIRECTORY , filename);
    }

    public static String readTestResourceFileFromNemsEventTemplatesDirectory(String filename) {
        return readTestResourceFile(Directory.NEMS_EVENT_TEMPLATES_DIRECTORY, filename);
    }

    @Getter
    public enum Directory {
        EHR_DIRECTORY("ehr"),
        NEMS_EVENT_TEMPLATES_DIRECTORY("nems-event-templates");

        private final String directory;

        Directory(String directory) {
            this.directory = directory;
        }
    }
}