package uk.nhs.prm.deduction.e2e.tests;

public enum Patient {
    SUSPENDED_WITH_EHR_AT_TPP("9693642937"),
    WITH_NO_9693643038_WHATEVER_THAT_MEANS("9693643038"),
    WITH_NO_9693795989_WHATEVER_THAT_MEANS("9693795989");

    private String nhsNumber;

    Patient(String nhsNumber) {
        this.nhsNumber = nhsNumber;
    }

    public String nhsNumber() {
        return nhsNumber;
    }
}
