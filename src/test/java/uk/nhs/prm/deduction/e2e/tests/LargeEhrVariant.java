package uk.nhs.prm.deduction.e2e.tests;

public enum LargeEhrVariant {
    SINGLE_ATTACHMENT(Patient.WITH_SINGLE_ATTACHMENT_LARGE_EHR);
 SUPER_LARGE(new Patient("9693796004"))
 HIGH_ATTACHMENT_COUNT(new Patient("9693796179"))

    private Patient patient;

    LargeEhrVariant(Patient patient) {
        this.patient = patient;
    }

    public Patient patient() {
        return patient;
    }
}
