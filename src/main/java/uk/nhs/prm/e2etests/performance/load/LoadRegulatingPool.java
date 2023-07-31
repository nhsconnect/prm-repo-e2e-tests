package uk.nhs.prm.e2etests.performance.load;

import uk.nhs.prm.e2etests.performance.reporting.Reportable;

import java.io.PrintStream;
import java.util.List;

public class LoadRegulatingPool<T extends Phased> implements FinitePool<T>, Reportable {
    private int count;
    private final Pool<T> sourcePool;
    private List<LoadPhase> phases;
    private int phaseIndex;
    private Long lastItemTimeMillis = null;

    public LoadRegulatingPool(Pool<T> sourcePool, List<LoadPhase> phases) {
        this.sourcePool = sourcePool;
        this.phases = phases;
        this.count = 0;
        this.phaseIndex = 0;
    }

    @Override
    public T next() {
        LoadPhase loadPhase = currentPhase();
        lastItemTimeMillis = loadPhase.applyDelay(lastItemTimeMillis);
        count++;
        loadPhase.incrementPhaseCount();

        T next = sourcePool.next();
        next.setPhase(loadPhase);
        return next;
    }

    @Override
    public boolean unfinished() {
        return currentPhase() != null;
    }

    @Override
    public void summariseTo(PrintStream out) {
        out.println();
        out.println("Total number of items of load provided: " + count);
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
