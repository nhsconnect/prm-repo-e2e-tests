package uk.nhs.prm.deduction.e2e.performance.load;

public interface FinitePool<T> extends Pool<T> {
    boolean unfinished();
}
