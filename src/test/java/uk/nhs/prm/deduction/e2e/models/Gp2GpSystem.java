package uk.nhs.prm.deduction.e2e.models;

import org.junit.jupiter.params.provider.Arguments;
import uk.nhs.prm.deduction.e2e.TestConfiguration;

import java.util.Map;
import java.util.stream.Stream;

public enum Gp2GpSystem {
    TPP_PTL_INT("M85019"),
    REPO_DEV("B85002"),
    REPO_TEST("B86041"),
    EMIS_PTL_INT("N82668");

    private String odsCode;

    Gp2GpSystem(String odsCode) {
        this.odsCode = odsCode;
    }

    public String odsCode() {
        return odsCode;
    }

    public String asidCode() {
        Map<String, String> knownAsidCodes = Map.of(
                "M85019", "200000000149", // TPP_PTL_INT
                "N82668", "200000000631", // EMIS_PTL_INT
                "B85002", "200000001613", // REPO dev
                "B86041", "200000001694" // REPO test
        );

        return knownAsidCodes.get(this.odsCode);
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

    public static Stream<Arguments> foundationSupplierSystems() {
        return Stream.of(
                Arguments.of(TPP_PTL_INT),
                Arguments.of(EMIS_PTL_INT));
    }
}
