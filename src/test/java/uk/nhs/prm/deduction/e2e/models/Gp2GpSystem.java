package uk.nhs.prm.deduction.e2e.models;

public enum Gp2GpSystem {
    TTP_DEV("M85019"), REPO_DEV("B85002"), EMIS_DEV("N82668");

    private String odsCode;

    Gp2GpSystem(String odsCode) {
        this.odsCode = odsCode;
    }

    public String odsCode() {
        return odsCode;
    }
}
