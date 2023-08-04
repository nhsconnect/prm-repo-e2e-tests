package uk.nhs.prm.e2etests.service;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import uk.nhs.prm.e2etests.exception.ServiceException;
import uk.nhs.prm.e2etests.model.EhrRequest;

import java.io.IOException;
import java.util.Map;

@Service
@Validated
public class TemplatingService {
    public static final String EHR_REQUEST_TEMPLATE = "ehr-request";
    public static final String CONTINUE_REQUEST_TEMPLATE = "continue-request";
    private final Handlebars handlebars;
    private Template template;

    @Autowired
    public TemplatingService(Handlebars handlebars) {
        this.handlebars = handlebars;
    }

    public String getContinueRequest(String outboundConversationId) {
        try {
            this.template = this.handlebars.compile(CONTINUE_REQUEST_TEMPLATE);
            final Map<String, String> templateContext = Map.of("outboundConversationId", outboundConversationId);
            return template.apply(templateContext);
        } catch (IOException exception) {
            throw new ServiceException(getClass().getName(), exception.getMessage());
        }
    }

    public String getEhrRequest(EhrRequest ehrRequest) {
        try {
            this.template = this.handlebars.compile(EHR_REQUEST_TEMPLATE);
            final Map<String, String> templateContext = Map.of(
                    "nhsNumber", ehrRequest.getNhsNumber(),
                    "outboundConversationId", ehrRequest.getOutboundConversationId(),
                    "messageId", ehrRequest.getMessageId(),
                    "newGpOdsCode", ehrRequest.getNewGpOdsCode()
            );
            return template.apply(templateContext);
        } catch (IOException exception) {
            throw new ServiceException(getClass().getName(), exception.getMessage());
        }
    }
}