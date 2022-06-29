package uk.nhs.prm.deduction.e2e.tests;

import uk.nhs.prm.deduction.e2e.TestConfiguration;

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

    public static Patient largeEhrAtEmisWithRepoMof(TestConfiguration config) {
        var environmentName = config.getEnvironmentName();
        if ("dev".equals(environmentName)) {
            return PATIENT_WITH_LARGE_EHR_AT_EMIS_WITH_MOF_SET_TO_REPO_DEV_ODS;
        }
        if ("test".equals(environmentName)) {
            return PATIENT_WITH_LARGE_EHR_AT_EMIS_WITH_MOF_SET_TO_REPO_TEST_ODS;
        }
        throw new IllegalStateException("Don't know about large EHR EMIS patient with repo mof in env: " + environmentName);
    }

}
