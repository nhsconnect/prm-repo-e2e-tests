package uk.nhs.prm.e2etests.model.templatecontext;

import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Getter;

import static uk.nhs.prm.e2etests.utility.ValidationUtility.ODS_CODE_REGEX;
import static uk.nhs.prm.e2etests.utility.ValidationUtility.UUID_REGEX;

@Getter
@Builder
public class LargeEhrFragmentTemplateContext implements TemplateContext {
    @Pattern(regexp = UUID_REGEX, message = "An invalid Inbound Conversation ID was provided.")
    private String inboundConversationId;

    @Pattern(regexp = ODS_CODE_REGEX, message = "An invalid ODS Code (new GP) was provided.")
    private String newGpOds;

    @Pattern(regexp = ODS_CODE_REGEX, message = "An invalid ODS Code (previous GP) was provided.")
    private String previousGpOds;
}