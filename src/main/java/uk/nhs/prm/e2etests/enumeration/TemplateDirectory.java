package uk.nhs.prm.e2etests.enumeration;

public enum TemplateDirectory {
    EHR_DIRECTORY("ehr"),
    HANDLEBARS_TEMPLATES("templates");

    public final String directoryName;

    TemplateDirectory(String directoryName) {
        this.directoryName = directoryName;
    }
}
