package uk.nhs.prm.e2etests.performance.load;

public interface FinitePool<T> extends Pool<T> {
    boolean unfinished();
}
