package uk.nhs.prm.e2etests.enumeration;

public enum Patient {
    SUSPENDED_WITH_EHR_AT_TPP("9693642937"),
    PATIENT_WITH_LARGE_EHR_AT_EMIS_WITH_MOF_SET_TO_REPO_DEV_ODS("9693643038"),
    PATIENT_WITH_LARGE_EHR_AT_EMIS_WITH_MOF_SET_TO_REPO_TEST_ODS("9694181372"),
    PATIENT_WITH_SMALL_EHR_IN_REPO_AND_MOF_SET_TO_TPP("9727018440"),
    WITH_SINGLE_FRAGMENT_LARGE_EHR("9727018076"),
    WITH_MULTIPLE_FRAGMENTS_LARGE_EHR("9693643038"),
    WITH_HIGH_FRAGMENT_COUNT_LARGE_EHR("9693796179"),
    WITH_SUPER_LARGE_EHR("9693796004"),
    WITH_LARGE_MEDICAL_HISTORY_EHR("9693796306");

    private final String nhsNumber;

    Patient(String nhsNumber) {
        this.nhsNumber = nhsNumber;
    }

    public String nhsNumber() {
        return nhsNumber;
    }

    public static Patient largeEhrAtEmisWithRepoMof(String nhsEnvironment) {
        return switch (nhsEnvironment) {
            case "dev" ->  PATIENT_WITH_LARGE_EHR_AT_EMIS_WITH_MOF_SET_TO_REPO_DEV_ODS;
            case "test" -> PATIENT_WITH_LARGE_EHR_AT_EMIS_WITH_MOF_SET_TO_REPO_TEST_ODS;
            default -> throw new IllegalStateException("Large EHR EMIS patient could not be found within the '" + nhsEnvironment + "' environment.");
        };
    }
}