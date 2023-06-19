package uk.nhs.prm.deduction.e2e.ehr_out_db;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;


@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "acknowledgements")
public class Acknowledgement {
    @Id
    private UUID messageId;
    private String acknowledgementTypeCode;
    private String acknowledgementDetail;
    private String service;
    private String referencedMessageId;
    private String messageRef;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
}