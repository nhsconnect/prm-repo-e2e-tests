package uk.nhs.prm.e2etests.model.templatecontext;

import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Getter;

import static uk.nhs.prm.e2etests.utility.ValidationUtility.ODS_CODE_REGEX;
import static uk.nhs.prm.e2etests.utility.ValidationUtility.UUID_REGEX;

@Getter
@Builder
public class LargeEhrFragmentOneContext implements TemplateContext {
    @Builder.Default
    @Pattern(regexp = UUID_REGEX, message = "An invalid Inbound Conversation ID was provided.")
    private String inboundConversationId;

    @Builder.Default
    @Pattern(regexp = UUID_REGEX, message = "An invalid Fragment Message ID was provided.")
    private String fragmentMessageId;

    @Builder.Default
    @Pattern(regexp = UUID_REGEX, message = "An invalid Fragment Two Message ID was provided.")
    private String fragmentTwoMessageId;

    @Builder.Default
    @Pattern(regexp = ODS_CODE_REGEX, message = "An invalid ODS Code (recipient) was provided.")
    private String recipientOdsCode;

    @Builder.Default
    @Pattern(regexp = ODS_CODE_REGEX, message = "An invalid ODS Code (sender) was provided.")
    private String senderOdsCode;
}