package uk.nhs.prm.e2etests.repository;

public interface ReadOnlyRepository<T, ID> {
    T findById(ID id);
}