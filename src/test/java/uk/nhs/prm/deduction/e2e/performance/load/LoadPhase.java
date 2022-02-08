package uk.nhs.prm.deduction.e2e.performance.load;

import uk.nhs.prm.deduction.e2e.timing.Sleeper;

import java.math.BigDecimal;

public class LoadPhase {
    private static final BigDecimal ONE_THOUSAND = new BigDecimal(1000);

    public final int totalCount;
    public final BigDecimal ratePerSecond;
    public int runningCount;

    public static LoadPhase atFlatRate(String ratePerSecond, int count) {
        return new LoadPhase(count, new BigDecimal(ratePerSecond));
    }

    private LoadPhase(int totalCount, BigDecimal ratePerSecond) {
        this.totalCount = totalCount;
        this.ratePerSecond = ratePerSecond;
        runningCount = 0;
    }

    public void incrementPhaseCount() {
        runningCount++;
    }

    public boolean finished() {
        return runningCount >= totalCount;
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
