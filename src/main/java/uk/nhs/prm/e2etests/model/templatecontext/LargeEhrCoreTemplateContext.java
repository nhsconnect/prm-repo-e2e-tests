package uk.nhs.prm.e2etests.model.templatecontext;

import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Getter;

import static uk.nhs.prm.e2etests.utility.ValidationUtility.*;

@Getter
@Builder
public class LargeEhrCoreTemplateContext implements TemplateContext {
    @Pattern(regexp = UUID_REGEX, message = "An invalid Inbound Conversation ID was provided.")
    private String inboundConversationId;

    @Pattern(regexp = UUID_REGEX, message = "An invalid Large EHR Core Message ID was provided.")
    private String largeEhrCoreMessageId;

    @Pattern(regexp = UUID_REGEX, message = "An invalid Fragment Message ID was provided.")
    private String fragmentMessageId;

    @Pattern(regexp = NHS_NUMBER_REGEX, message = "An invalid NHS Number was provided.")
    private String nhsNumber;

    @Pattern(regexp = ODS_CODE_REGEX, message = "An invalid ODS Code was provided.")
    private String recipientOdsCode;
}