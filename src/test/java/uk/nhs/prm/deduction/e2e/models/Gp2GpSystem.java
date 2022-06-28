package uk.nhs.prm.deduction.e2e.models;

import uk.nhs.prm.deduction.e2e.TestConfiguration;

public enum Gp2GpSystem {
    TPP_PTL_INT("M85019"), REPO_DEV("B85002"), REPO_TEST("B86041"), EMIS_PTL_INT("N82668");

    private String odsCode;

    Gp2GpSystem(String odsCode) {
        this.odsCode = odsCode;
    }

    public String odsCode() {
        return odsCode;
    }
}
