package uk.nhs.prm.e2etests.model.database;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import jakarta.persistence.Id;
import java.util.UUID;
import io.ebean.Model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity(name = "Acknowledgements")
public class Acknowledgement extends Model {
    @Id
    @Column(name = "message_id")
    private UUID messageId;
    @Column(name = "acknowledement_type_code")
    private String acknowledgementTypeCode;
    @Column(name = "acknowledgement_detail")
    private String acknowledgementDetail;
    @Column(name = "service")
    private String service;
    @Column(name = "referenced_message_id")
    private String referencedMessageId;
    @Column(name = "message_ref")
    private String messageRef;
    @Column(name = "created_at")
    private Timestamp createdAt;
    @Column(name = "updated_at")
    private Timestamp updatedAt;
    @Column(name = "deleted_at", nullable = true)
    private Timestamp deletedAt;

    public Acknowledgement(UUID messageId, String acknowledgementTypeCode, String acknowledgementDetail, String service, String referencedMessageId, String messageRef, Timestamp createdAt, Timestamp updatedAt, Timestamp deletedAt) {
        this.messageId = messageId;
        this.acknowledgementTypeCode = acknowledgementTypeCode;
        this.acknowledgementDetail = acknowledgementDetail;
        this.service = service;
        this.referencedMessageId = referencedMessageId;
        this.messageRef = messageRef;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }
}