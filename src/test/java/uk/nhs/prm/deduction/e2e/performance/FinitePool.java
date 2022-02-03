package uk.nhs.prm.deduction.e2e.performance;

public interface FinitePool<T> extends Pool<T> {
    boolean unfinished();
}
