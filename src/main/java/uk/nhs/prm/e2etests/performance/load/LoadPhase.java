package uk.nhs.prm.e2etests.performance.load;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static uk.nhs.prm.e2etests.utility.ThreadUtility.sleepFor;

public class LoadPhase {
    private static final BigDecimal ONE_THOUSAND = new BigDecimal(1000);

    public final int totalCount;
    public final BigDecimal ratePerSecond;
    public int runningCount;

    public static LoadPhase atFlatRate(int count, String ratePerSecond) {
        return new LoadPhase(count, new BigDecimal(ratePerSecond));
    }

    private LoadPhase(int totalCount, BigDecimal ratePerSecond) {
        this.totalCount = totalCount;
        this.ratePerSecond = ratePerSecond;
        runningCount = 0;
    }

    public String toString() {
        return totalCount + " events @" + ratePerSecond + "/s";
    }

    public void incrementPhaseCount() {
        runningCount++;
    }

    public boolean finished() {
        return runningCount >= totalCount;
    }

    private int targetDelayMilliseconds() {
        return ONE_THOUSAND.multiply(BigDecimal.ONE.divide(ratePerSecond, 3, RoundingMode.HALF_UP)).intValue();
    }

    /*
     * Intention of externalising time after last delay applied (returning and passing in next call) is so that
     * transitions between load phases work as expected.
     *
     * @return Time after delay applied
     */
    public Long applyDelay(Long lastItemTimeMillis) {
        long now = System.currentTimeMillis();

        if (lastItemTimeMillis == null) {
            return now;
        }
        long elapsed = now - lastItemTimeMillis;
        int requiredDelay = (int) (targetDelayMilliseconds() - elapsed);
        if (requiredDelay > 0) {
            return sleepFor(requiredDelay);
        }
        return now;
    }
}
