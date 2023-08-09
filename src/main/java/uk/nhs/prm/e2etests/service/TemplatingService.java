package uk.nhs.prm.e2etests.service;

import uk.nhs.prm.e2etests.model.templatecontext.NemsEventTemplateContext;
import uk.nhs.prm.e2etests.model.templatecontext.TemplateContext;
import uk.nhs.prm.e2etests.utility.NhsIdentityUtility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import uk.nhs.prm.e2etests.enumeration.TemplateVariant;
import uk.nhs.prm.e2etests.model.nems.NemsEventMessage;
import uk.nhs.prm.e2etests.exception.ServiceException;
import com.github.jknack.handlebars.Handlebars;
import org.springframework.stereotype.Service;
import jakarta.validation.Valid;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static uk.nhs.prm.e2etests.enumeration.TemplateDirectory.HANDLEBARS_TEMPLATES;
import static uk.nhs.prm.e2etests.utility.NhsIdentityUtility.randomNemsMessageId;
import static uk.nhs.prm.e2etests.utility.NhsIdentityUtility.randomNhsNumber;
import static uk.nhs.prm.e2etests.utility.ResourceUtility.readTestResourceFile;

@Service
@Validated
public class TemplatingService {
    private final Handlebars handlebars;

    @Autowired
    public TemplatingService(Handlebars handlebars) {
        this.handlebars = handlebars;
    }

    public List<String> getMultipleTemplatedStrings(Map<TemplateVariant, @Valid TemplateContext> variants) {
        return variants.entrySet()
                .stream()
                .map(element -> this.getTemplatedString(element.getKey(), element.getValue()))
                .toList();
    }

    public String getTemplatedString(TemplateVariant templateVariant, @Valid TemplateContext templateContext) {
        try {
            return this.handlebars.compileInline(readTestResourceFile(
                    HANDLEBARS_TEMPLATES, templateVariant.fileName
            )).apply(templateContext);
        } catch (IOException exception) {
            throw new ServiceException(getClass().getName(), exception.getMessage());
        }
    }

    public NemsEventMessage createNemsEventFromTemplate(TemplateVariant templateVariant, String suspendedPatientNhsNumber,
                                                         String nemsMessageId, String previousGp, String timestamp) {
        final NemsEventTemplateContext nemsEventTemplateContext = NemsEventTemplateContext.builder()
            .nhsNumber(suspendedPatientNhsNumber)
            .nemsMessageId(nemsMessageId)
            .previousGpOdsCode(previousGp)
            .lastUpdated(timestamp)
            .build();

        final String nemsEventMessageBody = this.getTemplatedString(
                templateVariant, nemsEventTemplateContext);

        return new NemsEventMessage(nemsMessageId, nemsEventMessageBody);
    }

    public Map<String, NemsEventMessage> getDLQNemsEventMessages() {
        final String timestamp = LocalDateTime.now().toString();
        final String liversedgeMedicalCentreOdsCode = "B85612";

        NemsEventMessage nhsNumberVerification = createNemsEventFromTemplate(TemplateVariant.NHS_NUMBER_VERIFICATION_FAIL,
                randomNhsNumber(),
                randomNemsMessageId(),
                liversedgeMedicalCentreOdsCode,
                timestamp);

        NemsEventMessage uriForManagingOrganizationNotPresent = createNemsEventFromTemplate(TemplateVariant.NO_REFERENCE_FOR_URI_FOR_MANAGING_ORGANIZATION,
                randomNhsNumber(),
                randomNemsMessageId(),
                liversedgeMedicalCentreOdsCode,
                timestamp);

        return Map.of(
            "nhsNumberVerification", nhsNumberVerification,
            "uriForManagingOrganizationNotPresent", uriForManagingOrganizationNotPresent
        );
    }
}