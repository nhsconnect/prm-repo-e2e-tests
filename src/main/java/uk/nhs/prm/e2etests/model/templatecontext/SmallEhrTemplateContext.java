package uk.nhs.prm.e2etests.model.templatecontext;

import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Getter;

import static uk.nhs.prm.e2etests.utility.TestDataUtility.randomUppercaseUuidAsString;
import static uk.nhs.prm.e2etests.utility.ValidationUtility.NHS_NUMBER_REGEX;
import static uk.nhs.prm.e2etests.utility.ValidationUtility.UUID_REGEX;

@Getter
@Builder
public class SmallEhrTemplateContext implements TemplateContext {

    private String inboundConversationId;

    @Pattern(regexp = NHS_NUMBER_REGEX, message = "An invalid NHS Number was provided.")
    private String nhsNumber;

    @Builder.Default
    @Pattern(regexp = UUID_REGEX, message = "An invalid Message ID was provided.")
    private String messageId = randomUppercaseUuidAsString();
}