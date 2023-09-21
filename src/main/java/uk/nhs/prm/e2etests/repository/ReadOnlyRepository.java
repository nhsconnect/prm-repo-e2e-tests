package uk.nhs.prm.e2etests.repository;

public interface ReadOnlyRepository<T, ID> {
    T findByMessageId(ID id);
}