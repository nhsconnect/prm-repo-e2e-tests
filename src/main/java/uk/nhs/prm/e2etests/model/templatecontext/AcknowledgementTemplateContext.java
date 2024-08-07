package uk.nhs.prm.e2etests.model.templatecontext;

import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

import static uk.nhs.prm.e2etests.utility.ValidationUtility.UUID_REGEX;

@Getter
@Builder
public class AcknowledgementTemplateContext implements TemplateContext {
    private String messageId;

    @Pattern(regexp = UUID_REGEX, message = "An invalid Conversation ID was provided.")
    private String inboundConversationId;
}
