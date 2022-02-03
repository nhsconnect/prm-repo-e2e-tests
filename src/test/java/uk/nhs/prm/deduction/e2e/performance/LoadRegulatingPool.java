package uk.nhs.prm.deduction.e2e.performance;

import java.io.PrintStream;

public class LoadRegulatingPool<T> implements FinitePool<T>, Reportable {
    private int count;
    private final Pool<T> sourcePool;
    private final int maxItems;

    public LoadRegulatingPool(Pool<T> sourcePool, int maxItems) {
        this.sourcePool = sourcePool;
        this.maxItems = maxItems;
        this.count = 0;
    }

    @Override
    public T next() {
        count++;
        return sourcePool.next();
    }

    @Override
    public boolean unfinished() {
        return count < maxItems;
    }

    @Override
    public void summariseTo(PrintStream out) {
        out.println("Number of items of load provided: " + count);
    }
}
