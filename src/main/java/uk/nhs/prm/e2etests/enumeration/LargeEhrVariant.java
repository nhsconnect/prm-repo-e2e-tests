package uk.nhs.prm.e2etests.enumeration;

public enum LargeEhrVariant {
    SINGLE_LARGE_FRAGMENT(Patient.WITH_SINGLE_FRAGMENT_LARGE_EHR, 2),
    MULTIPLE_LARGE_FRAGMENTS(Patient.WITH_MULTIPLE_FRAGMENTS_LARGE_EHR, 2),
    LARGE_MEDICAL_HISTORY(Patient.WITH_LARGE_MEDICAL_HISTORY_EHR, 2),
    HIGH_FRAGMENT_COUNT(Patient.WITH_HIGH_FRAGMENT_COUNT_LARGE_EHR, 150), // as of 2023-01-09 includes 402 message fragments
    SUPER_LARGE(Patient.WITH_SUPER_LARGE_EHR, 150);

    private final Patient patient;
    private final int timeoutMinutes;

    LargeEhrVariant(Patient patient, int timeoutMinutes) {
        this.patient = patient;
        this.timeoutMinutes = timeoutMinutes;
    }

    public Patient patient() {
        return patient;
    }

    public int timeoutMinutes() {
        return timeoutMinutes;
    }
}
