package uk.nhs.prm.e2etests.utility;

import org.springframework.web.client.ResourceAccessException;
import org.springframework.core.io.FileSystemResourceLoader;
import uk.nhs.prm.e2etests.enumeration.TemplateDirectory;
import org.springframework.core.io.ResourceLoader;

import java.nio.charset.StandardCharsets;
import java.io.IOException;

import static uk.nhs.prm.e2etests.enumeration.TemplateDirectory.EHR_DIRECTORY;

public final class ResourceUtility {
    private ResourceUtility() { }

    private static final ResourceLoader resourceLoader;

    static {
        resourceLoader = new FileSystemResourceLoader();
    }

    public static String readTestResourceFile(TemplateDirectory directory, String filename) {
        try {
            return resourceLoader
                    .getResource(String.format("classpath:%s/%s", directory.directoryName, filename))
                    .getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new ResourceAccessException(exception.getMessage());
        }
    }

    public static String readTestResourceFileFromEhrDirectory(String filename) {
        return readTestResourceFile(EHR_DIRECTORY , filename);
    }
}