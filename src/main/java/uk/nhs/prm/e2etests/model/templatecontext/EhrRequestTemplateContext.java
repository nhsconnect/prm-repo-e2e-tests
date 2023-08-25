package uk.nhs.prm.e2etests.model.templatecontext;

import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Getter;

import static uk.nhs.prm.e2etests.utility.TestDataUtility.randomUuidAsString;
import static uk.nhs.prm.e2etests.utility.ValidationUtility.*;

@Getter
@Builder
public class EhrRequestTemplateContext implements TemplateContext {
    @Pattern(regexp = NHS_NUMBER_REGEX, message = "An invalid NHS number was provided.")
    private String nhsNumber;

    @Builder.Default
    @Pattern(regexp = UUID_REGEX, message = "An invalid Outbound Conversation ID was provided.")
    private String outboundConversationId = randomUuidAsString();

    @Builder.Default
    @Pattern(regexp = UUID_REGEX, message = "An invalid Message ID was provided.")
    private String messageId = randomUuidAsString();

    @Pattern(regexp = ODS_CODE_REGEX, message = "An invalid ODS Code (sending) was provided.")
    private String sendingOdsCode;

    @Pattern(regexp = ODS_CODE_REGEX, message = "An invalid ODS Code (receiving) was provided.")
    private String receivingOdsCode;

    @Pattern(regexp = ASID_REGEX, message = "An invalid ASID Code (recipient) was provided.")
    private String asidCode;
}
