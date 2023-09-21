package uk.nhs.prm.e2etests.model.templatecontext;

import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Getter;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static uk.nhs.prm.e2etests.utility.ValidationUtility.NHS_NUMBER_REGEX;
import static uk.nhs.prm.e2etests.utility.ValidationUtility.ODS_CODE_REGEX;
import static uk.nhs.prm.e2etests.utility.ValidationUtility.UUID_REGEX;

@Getter
@Builder
public class NemsEventTemplateContext implements TemplateContext {
    private static final String DEFAULT_TIMESTAMP = ZonedDateTime.now(ZoneOffset.UTC).toString();

    @Pattern(regexp = UUID_REGEX, message = "An invalid NEMS message ID was provided.")
    private String nemsMessageId;

    @Builder.Default
    private String lastUpdated = DEFAULT_TIMESTAMP;

    @Pattern(regexp = NHS_NUMBER_REGEX, message = "An invalid NHS Number was provided.")
    private String nhsNumber;

    @Pattern(regexp = ODS_CODE_REGEX, message = "An invalid Recipient Ods Code was provided.")
    private String recipientOdsCode;
}
