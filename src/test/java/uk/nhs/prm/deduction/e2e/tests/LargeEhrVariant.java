package uk.nhs.prm.deduction.e2e.tests;

public enum LargeEhrVariant {
    SINGLE_ATTACHMENT(Patient.WITH_SINGLE_ATTACHMENT_LARGE_EHR);

    private Patient patient;

    LargeEhrVariant(Patient patient) {
        this.patient = patient;
    }

    public Patient patient() {
        return patient;
    }
}
