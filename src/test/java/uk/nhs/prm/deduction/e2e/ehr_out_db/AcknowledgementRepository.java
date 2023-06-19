package uk.nhs.prm.deduction.e2e.ehr_out_db;

import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@NoRepositoryBean
public interface AcknowledgementRepository extends Repository<Acknowledgement, UUID> {
    Optional<Acknowledgement> findByMessageId(UUID id);
    List<Acknowledgement> findAll();
}