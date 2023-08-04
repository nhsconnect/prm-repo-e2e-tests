package uk.nhs.prm.e2etests.model;

import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Getter;

import static uk.nhs.prm.e2etests.utility.ValidationUtility.*;

@Getter
@Builder
public class EhrRequest {
    @Pattern(regexp = NHS_NUMBER_REGEX, message = "An invalid NHS number was provided.")
    private String nhsNumber;
    @Pattern(regexp = UUID_REGEX, message = "An invalid Outbound Conversation ID was provided.")
    private String outboundConversationId;
    @Pattern(regexp = UUID_REGEX, message = "An invalid Message ID was provided.")
    private String messageId;
    @Pattern(regexp = ODS_CODE_REGEX, message = "An invalid ODS Code was provided.")
    private String newGpOdsCode;
}
