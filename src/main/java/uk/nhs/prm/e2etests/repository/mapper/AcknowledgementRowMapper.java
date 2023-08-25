package uk.nhs.prm.e2etests.repository.mapper;

import uk.nhs.prm.e2etests.model.database.Acknowledgement;
import org.springframework.stereotype.Component;
import org.springframework.jdbc.core.RowMapper;

import java.sql.SQLException;
import java.sql.ResultSet;
import java.util.UUID;

@Component
public class AcknowledgementRowMapper implements RowMapper<Acknowledgement> {
    @Override
    public Acknowledgement mapRow(ResultSet rs, int rowNum) throws SQLException {
        return Acknowledgement.builder()
                .messageId(UUID.fromString(rs.getString("message_id")))
                .acknowledgementTypeCode(rs.getString("acknowledgement_type_code"))
                .acknowledgementDetail(rs.getString("acknowledgement_detail"))
                .service(rs.getString("service"))
                .referencedMessageId(rs.getString("message_ref"))
                .createdAt(rs.getTimestamp("created_at"))
                .updatedAt(rs.getTimestamp("updated_at"))
                .deletedAt(rs.getTimestamp("deleted_at"))
                .build();
    }
}