package uk.nhs.prm.e2etests.model.database;

import lombok.*;

import java.sql.Timestamp;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class Acknowledgement {
    private UUID messageId;
    private String acknowledgementTypeCode;
    private String acknowledgementDetail;
    private String service;
    private String referencedMessageId;
    private String messageRef;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private Timestamp deletedAt;
}