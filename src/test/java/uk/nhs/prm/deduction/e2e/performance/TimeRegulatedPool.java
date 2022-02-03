package uk.nhs.prm.deduction.e2e.performance;

public class TimeRegulatedPool<T> implements Pool<T> {
    private Pool<T> sourcePool;

    public TimeRegulatedPool(Pool<T> sourcePool) {
        this.sourcePool = sourcePool;
    }

    @Override
    public T next() {
        return sourcePool.next();
    }
}
