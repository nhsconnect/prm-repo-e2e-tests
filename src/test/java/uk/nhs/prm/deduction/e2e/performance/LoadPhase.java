package uk.nhs.prm.deduction.e2e.performance;

import uk.nhs.prm.deduction.e2e.timng.Sleeper;

import java.math.BigDecimal;

public class LoadPhase {
    private static final BigDecimal ONE_THOUSAND = new BigDecimal(1000);

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

    public boolean finished() {
        return count >= maxItems;
    }

    private int targetDelayMilliseconds() {
        return ONE_THOUSAND.multiply(BigDecimal.ONE.divide(ratePerSecond)).intValue();
    }

    /*
     * @return Time after delay applied
     */
    Long applyDelay(long now, Sleeper sleeper, Long lastItemTimeMillis) {
        if (lastItemTimeMillis == null) {
            return now;
        }
        var elapsed = now - lastItemTimeMillis;
        int requiredDelay = (int) (targetDelayMilliseconds() - elapsed);
        if (requiredDelay > 0) {
            sleeper.sleep(requiredDelay);
        }
        return now + requiredDelay;
    }
}
