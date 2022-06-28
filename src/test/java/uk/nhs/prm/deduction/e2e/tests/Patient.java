package uk.nhs.prm.deduction.e2e.tests;

public enum Patient {
    SUSPENDED_WITH_EHR_AT_TPP("9693642937");

    private String nhsNumber;

    Patient(String nhsNumber) {
        this.nhsNumber = nhsNumber;
    }

    public String nhsNumber() {
        return nhsNumber;
    }
}
