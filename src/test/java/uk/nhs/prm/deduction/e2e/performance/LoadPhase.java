package uk.nhs.prm.deduction.e2e.performance;

import java.math.BigDecimal;

public class LoadPhase {

    public final int maxItems;
    public final BigDecimal ratePerSecond;
    public final BigDecimal rampRatePerSecondSquared;
    public int count;

    public static LoadPhase atFlatRate(String ratePerSecond, int count) {
        return new LoadPhase(count, new BigDecimal(ratePerSecond), BigDecimal.ZERO);
    }

    private LoadPhase(int maxItems, BigDecimal ratePerSecond, BigDecimal rampRatePerSecondSquared) {
        this.maxItems = maxItems;
        this.ratePerSecond = ratePerSecond;
        this.rampRatePerSecondSquared = rampRatePerSecondSquared;
        count = 0;
    }

    public void incrementPhaseCount() {
        count++;
    }
}
