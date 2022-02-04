package uk.nhs.prm.deduction.e2e.performance;

import java.math.BigDecimal;

public class LoadPhase {

    public final int durationSeconds;
    public final BigDecimal ratePerSecond;
    public final BigDecimal rampRatePerSecondSquared;

    public static LoadPhase atFlatRate(int durationSeconds, String ratePerSecond) {
        return new LoadPhase(durationSeconds, new BigDecimal(ratePerSecond), BigDecimal.ZERO);
    }

    private LoadPhase(int durationSeconds, BigDecimal ratePerSecond, BigDecimal rampRatePerSecondSquared) {
        this.durationSeconds = durationSeconds;
        this.ratePerSecond = ratePerSecond;
        this.rampRatePerSecondSquared = rampRatePerSecondSquared;
    }
}
