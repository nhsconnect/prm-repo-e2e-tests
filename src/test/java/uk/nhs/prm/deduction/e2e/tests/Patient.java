package uk.nhs.prm.deduction.e2e.tests;

public enum Patient {
    SUSPENDED_WITH_EHR_AT_TPP("9693642937"),
    PATIENT_WITH_LARGE_EHR_AT_EMIS_WITH_MOF_SET_TO_REPO_DEV_ODS("9693643038"),
    PATIENT_WITH_LARGE_EHR_AT_EMIS_WITH_MOF_SET_TO_REPO_TEST_ODS("9694181372"),
    WITH_NO_9693795989_WHATEVER_THAT_MEANS("9693795989");

    private String nhsNumber;

    Patient(String nhsNumber) {
        this.nhsNumber = nhsNumber;
    }

    public String nhsNumber() {
        return nhsNumber;
    }
}
