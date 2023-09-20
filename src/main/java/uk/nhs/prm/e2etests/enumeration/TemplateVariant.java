package uk.nhs.prm.e2etests.enumeration;

public enum TemplateVariant {
    // EHR Message
    CONTINUE_REQUEST("continue-request.hbs"),
    EHR_REQUEST("ehr-request.hbs"),
    SMALL_EHR("small-ehr.hbs"),
    SMALL_EHR_WITH_99_ATTACHMENTS("small-ehr-with-99-attachments.hbs"),
    SMALL_EHR_WITH_LINEBREAKS("small-ehr-with-linebreaks.hbs"),
    SMALL_EHR_WITHOUT_LINEBREAKS("small-ehr-without-linebreaks.hbs"),
    LARGE_EHR_CORE("large-ehr-core.hbs"),
    LARGE_EHR_CORE_VARIABLE_MANIFEST("large-ehr-core-variable-manifest.hbs"),
    LARGE_EHR_FRAGMENT_NO_REF_4MB("large-ehr-fragment-no-ref-4mb.hbs"),
    LARGE_EHR_FRAGMENT_WITH_REF("large-ehr-fragment-with-ref.hbs"),
    LARGE_EHR_FRAGMENT_NO_REF("large-ehr-fragment-no-ref.hbs"),
    POSITIVE_ACKNOWLEDGEMENT("positive-acknowledgement.hbs"),
    NEGATIVE_ACKNOWLEDGEMENT("negative-acknowledgement.hbs"),

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