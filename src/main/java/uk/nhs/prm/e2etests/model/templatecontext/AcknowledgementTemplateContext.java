package uk.nhs.prm.e2etests.model.templatecontext;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class AcknowledgementTemplateContext implements TemplateContext {
    @Builder.Default
    private UUID messageId = UUID.randomUUID();
}
