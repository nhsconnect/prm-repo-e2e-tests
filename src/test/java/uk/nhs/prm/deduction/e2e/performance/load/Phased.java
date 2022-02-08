package uk.nhs.prm.deduction.e2e.performance.load;

public interface Phased {
    void setPhase(LoadPhase phase);
    LoadPhase phase();
}
