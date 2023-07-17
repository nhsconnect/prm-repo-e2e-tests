package uk.nhs.prm.e2etests.performance.load;

public interface Phased {
    void setPhase(LoadPhase phase);
    LoadPhase phase();
}
