package uk.nhs.prm.deduction.e2e.performance.load;

import uk.nhs.prm.deduction.e2e.performance.reporting.Reportable;
import uk.nhs.prm.deduction.e2e.timing.Timer;
import uk.nhs.prm.deduction.e2e.timing.Sleeper;

import java.io.PrintStream;
import java.util.List;

public class LoadRegulatingPool<T> implements FinitePool<T>, Reportable {
    private int count;
    private final Pool<T> sourcePool;
    private List<LoadPhase> phases;
    private int phaseIndex;
    private Timer timer;
    private Sleeper sleeper;
    private Long lastItemTimeMillis = null;

    public LoadRegulatingPool(Pool<T> sourcePool, List<LoadPhase> phases) {
        this(sourcePool, phases, new Timer(), new Sleeper());
    }

    public LoadRegulatingPool(Pool<T> sourcePool, List<LoadPhase> phases, Timer timer, Sleeper sleeper) {
        this.sourcePool = sourcePool;
        this.phases = phases;
        this.timer = timer;
        this.sleeper = sleeper;
        this.count = 0;
        this.phaseIndex = 0;
    }

    @Override
    public T next() {
        long nowMilliseconds = timer.milliseconds();
        lastItemTimeMillis = currentPhase().applyDelay(nowMilliseconds, sleeper, lastItemTimeMillis);
        count++;
        currentPhase().incrementPhaseCount();
        return sourcePool.next();
    }

    @Override
    public boolean unfinished() {
        return currentPhase() != null;
    }

    @Override
    public void summariseTo(PrintStream out) {
        out.println("Number of items of load provided: " + count);
    }

    private LoadPhase currentPhase() {
        LoadPhase phase = phases.get(phaseIndex);
        if (phase.finished()) {
            if (phases.size() > phaseIndex + 1) {
                phaseIndex++;
                return phases.get(phaseIndex);
            }
            return null;
        }
        return phase;
    }
}
