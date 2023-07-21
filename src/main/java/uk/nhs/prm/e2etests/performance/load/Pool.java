package uk.nhs.prm.e2etests.performance.load;

public interface Pool<T> {
    T next();
}
