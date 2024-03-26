package uk.nhs.prm.e2etests.model.database;

import lombok.Getter;
import lombok.Builder;
import lombok.AllArgsConstructor;

import java.sql.Timestamp;
import java.util.UUID;

/**
 * @deprecated PRMT-4670 old Transfer Tracker DB logic - to be removed
 */
@Deprecated(since="2.0.0", forRemoval = true)
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