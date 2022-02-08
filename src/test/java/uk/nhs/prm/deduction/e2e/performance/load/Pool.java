package uk.nhs.prm.deduction.e2e.performance.load;

public interface Pool<T> {
    T next();
}
