package uk.nhs.prm.e2etests.enumeration;

public enum TemplateVariant {
    // EHR Message
    CONTINUE_REQUEST("continue-request.hbs"),
    EHR_REQUEST("ehr-request.hbs"),
    SMALL_EHR("small-ehr.hbs"),
    SMALL_EHR_WITH_LINEBREAKS("small-ehr-with-linebreaks.hbs"),
    SMALL_EHR_WITHOUT_LINEBREAKS("small-ehr-without-linebreaks.hbs"),
    LARGE_EHR_CORE("large-ehr-core.hbs"),
    LARGE_EHR_FRAGMENT_ONE("large-ehr-fragment-1.hbs"),
    LARGE_EHR_FRAGMENT_TWO("large-ehr-fragment-2.hbs"),

    // NEMS event xml
    CHANGE_OF_GP_NON_SUSPENSION("change-of-gp-non-suspension.hbs"),
    CHANGE_OF_GP_RE_REGISTRATION("change-of-gp-re-registration.hbs"),
    CHANGE_OF_GP_SUSPENSION("change-of-gp-suspension.hbs"),
    NHS_NUMBER_VERIFICATION_FAIL("nhs-number-verification-fail.hbs"),
    NO_REFERENCE_FOR_URI_FOR_MANAGING_ORGANIZATION("no-reference-for-uri-for-managing-organization.hbs");

    public final String fileName;

    TemplateVariant(String fileName) {
        this.fileName = fileName;
    }
}