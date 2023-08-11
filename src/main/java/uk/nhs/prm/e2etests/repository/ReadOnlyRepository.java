package uk.nhs.prm.e2etests.repository;

public interface ReadOnlyRepository<T, ID> {
    public T findById(ID id);
}
