package uk.nhs.prm.e2etests.repository;

import org.springframework.beans.factory.annotation.Autowired;
import uk.nhs.prm.e2etests.model.database.Acknowledgement;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import lombok.AllArgsConstructor;

import java.util.UUID;

@Repository
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class EhrOutDatabaseAcknowledgeRepository implements ReadOnlyRepository<Acknowledgement, UUID> {
    private final JdbcTemplate jdbcTemplate;
    private final AcknowledgementRowMapper acknowledgementRowMapper;

    @Override
    public Acknowledgement findById(UUID uuid) {
        return jdbcTemplate.queryForObject(
                "SELECT * FROM Acknowledgements WHERE message_id = ?",
                acknowledgementRowMapper,
                10);
    }
}
