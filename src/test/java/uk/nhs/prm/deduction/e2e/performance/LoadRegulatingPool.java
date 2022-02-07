package uk.nhs.prm.deduction.e2e.performance;

import uk.nhs.prm.deduction.e2e.timing.Timer;
import uk.nhs.prm.deduction.e2e.timng.Sleeper;

import java.io.PrintStream;
import java.math.BigDecimal;
import java.util.List;

public class LoadRegulatingPool<T> implements FinitePool<T>, Reportable {
    private static final BigDecimal ONE_THOUSAND = new BigDecimal(1000);

    private int count;
    private final Pool<T> sourcePool;
    private List<LoadPhase> phases;
    private final int phaseIndex;
    private Long lastItemTimeMillis = null;
    private int phaseCount;
    private Timer timer;
    private Sleeper sleeper;

    public LoadRegulatingPool(Pool<T> sourcePool, List<LoadPhase> phases) {
        this(sourcePool, phases, new Timer(), new Sleeper());
    }

    public LoadRegulatingPool(Pool<T> sourcePool, List<LoadPhase> phases, Timer timer, Sleeper sleeper) {
        this.sourcePool = sourcePool;
        this.phases = phases;
        this.timer = timer;
        this.sleeper = sleeper;
        this.count = 0;
        this.phaseCount = 0;
        this.phaseIndex = 0;
    }

    @Override
    public T next() {
        long nowMilliseconds = timer.milliseconds();
        applyDelay(nowMilliseconds);
        count++;
        phaseCount++;
        return sourcePool.next();
    }

    private void applyDelay(long startTime) {
        if (lastItemTimeMillis == null) {
            lastItemTimeMillis = startTime;
            return;
        }
        var elapsed = startTime - lastItemTimeMillis;
        int requiredDelay = (int) (targetDelayMilliseconds() - elapsed);
        if (requiredDelay > 0) {
            sleeper.sleep(requiredDelay);
        }
        lastItemTimeMillis = startTime + requiredDelay;
    }

    private int targetDelayMilliseconds() {
        return ONE_THOUSAND.multiply(BigDecimal.ONE.divide(currentPhase().ratePerSecond)).intValue();
    }

    @Override
    public boolean unfinished() {
        return !phaseFinished();
    }

    private boolean phaseFinished() {
        return phaseCount >= currentPhase().maxItems;
    }

    @Override
    public void summariseTo(PrintStream out) {
        out.println("Number of items of load provided: " + count);
    }

    private LoadPhase currentPhase() {
        return phases.get(phaseIndex);
    }
}
