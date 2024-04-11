package uk.nhs.prm.e2etests.model.templatecontext;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Getter;

import static uk.nhs.prm.e2etests.utility.TestDataUtility.randomUppercaseUuidAsString;
import static uk.nhs.prm.e2etests.utility.ValidationUtility.ODS_CODE_REGEX;
import static uk.nhs.prm.e2etests.utility.ValidationUtility.UUID_REGEX;

@Getter
@Builder
public class ContinueRequestTemplateContext implements TemplateContext {
    @Pattern(regexp = UUID_REGEX, message = "An invalid Outbound Conversation ID was provided.")
    @NotBlank(message = "The outbound Conversation ID cannot be blank.")
    private String outboundConversationId;

    @Builder.Default
    @Pattern(regexp = UUID_REGEX, message = "An invalid Message ID was provided.")
    @NotBlank(message = "The Message ID cannot be blank.")
    private String messageId = randomUppercaseUuidAsString();

    @Pattern(regexp = ODS_CODE_REGEX, message = "An invalid ODS Code (recipient) was provided.")
    @NotBlank(message = "The ODS Code (recipient) cannot be blank.")
    private String recipientOdsCode;

    @Pattern(regexp = ODS_CODE_REGEX, message = "An invalid ODS Code (sender) was provided.")
    @NotBlank(message = "The ODS Code (sender) cannot be blank.")
    private String senderOdsCode;
}
