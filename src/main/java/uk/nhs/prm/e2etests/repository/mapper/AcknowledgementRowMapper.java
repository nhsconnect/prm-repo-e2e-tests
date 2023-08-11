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
        Acknowledgement acknowledgement = new Acknowledgement();

        acknowledgement.setMessageId(UUID.fromString(rs.getString("message_id")));
        acknowledgement.setAcknowledgementTypeCode(rs.getString("acknowledgement_type_code"));
        acknowledgement.setAcknowledgementDetail(rs.getString("acknowledgement_detail"));
        acknowledgement.setService(rs.getString("service"));
        acknowledgement.setReferencedMessageId(rs.getString("referenced_message_id"));
        acknowledgement.setMessageRef(rs.getString("message_ref"));
        acknowledgement.setCreatedAt(rs.getTimestamp("created_at"));
        acknowledgement.setUpdatedAt(rs.getTimestamp("updated_at"));
        acknowledgement.setDeletedAt(rs.getTimestamp("deleted_at"));

        return acknowledgement;
    }
}