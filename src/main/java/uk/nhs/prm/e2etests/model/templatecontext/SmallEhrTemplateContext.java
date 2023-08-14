package uk.nhs.prm.e2etests.model.templatecontext;

import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

import static uk.nhs.prm.e2etests.utility.ValidationUtility.NHS_NUMBER_REGEX;
import static uk.nhs.prm.e2etests.utility.ValidationUtility.UUID_REGEX;

@Getter
@Builder
public class SmallEhrTemplateContext implements TemplateContext {
    @Builder.Default
    @Pattern(regexp = UUID_REGEX, message = "An invalid Conversation ID was provided.")
    private String inboundConversationId = UUID.randomUUID().toString().toUpperCase();

    @Pattern(regexp = NHS_NUMBER_REGEX, message = "An invalid NHS Number was provided.")
    private String nhsNumber;

    @Builder.Default
    @Pattern(regexp = UUID_REGEX, message = "An invalid Message ID was provided.")
    private String messageId = UUID.randomUUID().toString().toUpperCase();
}