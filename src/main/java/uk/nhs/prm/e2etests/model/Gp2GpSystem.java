package uk.nhs.prm.e2etests.model;

import uk.nhs.prm.e2etests.TestConfiguration;

public enum Gp2GpSystem {
    TPP_PTL_INT("M85019"),
    REPO_DEV("B85002"),
    REPO_TEST("B86041"),
    EMIS_PTL_INT("N82668");

    private final String odsCode;

    Gp2GpSystem(String odsCode) {
        this.odsCode = odsCode;
    }

    public String odsCode() {
        return odsCode;
    }

    public static Gp2GpSystem repoInEnv(TestConfiguration config) {
        var environmentName = config.getEnvironmentName();
        if ("dev".equals(environmentName)) {
            return REPO_DEV;
        }
        if ("test".equals(environmentName)) {
            return REPO_TEST;
        }
        throw new IllegalStateException("Don't know about repo in environment: " + environmentName);
    }
}
