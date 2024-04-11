package uk.nhs.prm.e2etests.model.templatecontext;

import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Getter;

import static uk.nhs.prm.e2etests.utility.TestDataUtility.randomUppercaseUuidAsString;
import static uk.nhs.prm.e2etests.utility.ValidationUtility.NHS_NUMBER_REGEX;
import static uk.nhs.prm.e2etests.utility.ValidationUtility.UUID_REGEX;
import static uk.nhs.prm.e2etests.utility.ValidationUtility.ODS_CODE_REGEX;
import static uk.nhs.prm.e2etests.utility.ValidationUtility.ASID_REGEX;

@Getter
@Builder
public class EhrRequestTemplateContext implements TemplateContext {
    @Pattern(regexp = NHS_NUMBER_REGEX, message = "An invalid NHS number was provided.")
    private String nhsNumber;

    @Builder.Default
    @Pattern(regexp = UUID_REGEX, message = "An invalid Outbound Conversation ID was provided.")
    private String outboundConversationId = randomUppercaseUuidAsString();

    @Builder.Default
    @Pattern(regexp = UUID_REGEX, message = "An invalid Message ID was provided.")
    private String messageId = randomUppercaseUuidAsString();

    @Pattern(regexp = ODS_CODE_REGEX, message = "An invalid ODS Code (sender) was provided.")
    private String senderOdsCode;

    @Pattern(regexp = ODS_CODE_REGEX, message = "An invalid ODS Code (recipient) was provided.")
    private String recipientOdsCode;

    @Pattern(regexp = ASID_REGEX, message = "An invalid ASID Code (recipient) was provided.")
    private String asidCode;
}
