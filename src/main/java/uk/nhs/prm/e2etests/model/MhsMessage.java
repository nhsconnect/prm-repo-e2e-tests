package uk.nhs.prm.e2etests.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MhsMessage {
    private String conversationId;
    private String messageId;
    private String message;
}