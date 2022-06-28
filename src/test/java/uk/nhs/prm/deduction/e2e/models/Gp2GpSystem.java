package uk.nhs.prm.deduction.e2e.models;

public enum Gp2GpSystem {
    TPP_PTL_INT("M85019"), REPO_DEV("B85002"), EMIS_PTL_INT("N82668");

    private String odsCode;

    Gp2GpSystem(String odsCode) {
        this.odsCode = odsCode;
    }

    public String odsCode() {
        return odsCode;
    }
}
