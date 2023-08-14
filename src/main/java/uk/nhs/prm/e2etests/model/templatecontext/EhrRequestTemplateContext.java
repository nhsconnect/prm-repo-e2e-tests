package uk.nhs.prm.e2etests.model.templatecontext;

import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

import static uk.nhs.prm.e2etests.utility.ValidationUtility.*;

@Getter
@Builder
public class EhrRequestTemplateContext implements TemplateContext {
    @Pattern(regexp = NHS_NUMBER_REGEX, message = "An invalid NHS number was provided.")
    private String nhsNumber;

    @Builder.Default
    @Pattern(regexp = UUID_REGEX, message = "An invalid Outbound Conversation ID was provided.")
    private String outboundConversationId = UUID.randomUUID().toString();

    @Builder.Default
    @Pattern(regexp = UUID_REGEX, message = "An invalid Message ID was provided.")
    private String messageId = UUID.randomUUID().toString();

    @Pattern(regexp = ODS_CODE_REGEX, message = "An invalid ODS Code (new GP) was provided.")
    private String newGpOdsCode;

    @Pattern(regexp = ASID_REGEX, message = "An invalid ASID Code (new GP) was provided.")
    private String asidCode;
}
