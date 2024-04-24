package uk.nhs.prm.e2etests.enumeration;

import uk.nhs.prm.e2etests.annotation.Debt;
import uk.nhs.prm.e2etests.exception.InvalidNhsEnvironmentException;

import static uk.nhs.prm.e2etests.annotation.Debt.Priority.MEDIUM;

@Debt(comment = "Ideally we would not have the ods codes attached to these environments and could break them out" +
        "into properties files. There are several cases in the codebase where the environments (dev, preprod, perf) etc" +
        "are being referred to as strings. This enum would be an ideal candidate to expand out.", priority = MEDIUM)
public enum Gp2GpSystem {
    REPO_DEV("B85002", "200000001613"),
    REPO_TEST("B86041", "200000001694"),
    EMIS_PTL_INT("N82668", "200000000631"),
    TPP_PTL_INT("M85019", "200000000149");

    private final String odsCode;
    private final String asidCode;

    Gp2GpSystem(String odsCode, String asidCode) {
        this.odsCode = odsCode;
        this.asidCode = asidCode;
    }

    public String odsCode() {
        return odsCode;
    }

    public String asidCode() {
        return asidCode;
    }

    @Deprecated // Use NhsProperties.getRepoOdsCode() instead
    public static Gp2GpSystem repoInEnv(String nhsEnvironment) {
        switch (nhsEnvironment) {
            case "dev" -> { return REPO_DEV; }
            case "test" -> { return REPO_TEST; }
            default -> throw new InvalidNhsEnvironmentException();
        }
    }
}