package uk.nhs.prm.e2etests.service;

import uk.nhs.prm.e2etests.enumeration.TemplateVariant;
import uk.nhs.prm.e2etests.model.templatecontext.SmallEhrTemplateContext;
import uk.nhs.prm.e2etests.model.database.TransferTrackerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import uk.nhs.prm.e2etests.enumeration.Gp2GpSystem;
import uk.nhs.prm.e2etests.queue.SimpleAmqpQueue;
import org.springframework.stereotype.Service;
import jakarta.validation.constraints.Pattern;
import lombok.extern.log4j.Log4j2;
import lombok.AllArgsConstructor;
import jakarta.validation.Valid;

import static uk.nhs.prm.e2etests.enumeration.TransferTrackerStatus.EHR_TRANSFER_TO_REPO_COMPLETE;
import static uk.nhs.prm.e2etests.enumeration.TemplateVariant.SMALL_EHR_WITHOUT_LINEBREAKS;
import static uk.nhs.prm.e2etests.enumeration.TransferTrackerStatus.EHR_REQUEST_SENT;
import static uk.nhs.prm.e2etests.utility.NhsIdentityUtility.randomNemsMessageId;
import static uk.nhs.prm.e2etests.utility.ValidationUtility.NHS_NUMBER_REGEX;

@Log4j2
@Service
@Validated
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class RepoService {
    private final SimpleAmqpQueue mhsInboundQueue;
    private final TemplatingService templatingService;
    private final TransferTrackerService transferTrackerService;

    public void addSmallEhrToEhrRepo(@Valid @Pattern(regexp = NHS_NUMBER_REGEX) String nhsNumber, TemplateVariant templateVariant) {
        final SmallEhrTemplateContext smallEhrTemplateContext = SmallEhrTemplateContext.builder()
                .nhsNumber(nhsNumber)
                .build();
        final String inboundConversationId = smallEhrTemplateContext.getInboundConversationId();
        final String smallEhrMessage = this.templatingService.getTemplatedString(templateVariant, smallEhrTemplateContext);

        this.transferTrackerService.save(TransferTrackerRecord.builder()
                .conversationId(inboundConversationId)
                .nhsNumber(nhsNumber)
                .sourceGp(Gp2GpSystem.TPP_PTL_INT.odsCode())
                .nemsMessageId(randomNemsMessageId())
                .state(EHR_REQUEST_SENT.status).build());

        this.mhsInboundQueue.sendMessage(smallEhrMessage, inboundConversationId);
        this.transferTrackerService.waitForStatusMatching(inboundConversationId, EHR_TRANSFER_TO_REPO_COMPLETE.status);
        log.info("Small EHR with Conversation ID {} added to the repository successfully.", inboundConversationId);
    }
}