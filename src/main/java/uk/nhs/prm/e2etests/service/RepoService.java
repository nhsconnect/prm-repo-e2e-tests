package uk.nhs.prm.e2etests.service;

import org.apache.commons.lang3.time.StopWatch;
import uk.nhs.prm.e2etests.model.templatecontext.LargeEhrCoreVariableManifestTemplateContext;
import uk.nhs.prm.e2etests.model.templatecontext.LargeEhrFragmentNoReferencesContext;
import uk.nhs.prm.e2etests.model.templatecontext.SmallEhrTemplateContext;
import uk.nhs.prm.e2etests.model.database.TransferTrackerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import uk.nhs.prm.e2etests.enumeration.TemplateVariant;
import uk.nhs.prm.e2etests.enumeration.Gp2GpSystem;
import uk.nhs.prm.e2etests.queue.SimpleAmqpQueue;
import org.springframework.stereotype.Service;
import jakarta.validation.constraints.Pattern;
import lombok.extern.log4j.Log4j2;
import lombok.AllArgsConstructor;
import jakarta.validation.Valid;

import java.util.stream.Stream;
import java.util.List;
import java.util.UUID;

import static uk.nhs.prm.e2etests.enumeration.TemplateVariant.*;
import static uk.nhs.prm.e2etests.enumeration.TransferTrackerStatus.LARGE_EHR_CONTINUE_REQUEST_SENT;
import static uk.nhs.prm.e2etests.enumeration.TransferTrackerStatus.EHR_TRANSFER_TO_REPO_COMPLETE;
import static uk.nhs.prm.e2etests.enumeration.TransferTrackerStatus.EHR_REQUEST_SENT;
import static uk.nhs.prm.e2etests.utility.TestDataUtility.randomNemsMessageId;
import static uk.nhs.prm.e2etests.enumeration.TemplateVariant.SMALL_EHR_WITHOUT_LINEBREAKS;
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

    public void addLargeEhrWithVariableManifestToRepo(@Valid @Pattern(regexp = NHS_NUMBER_REGEX) String nhsNumber, int numberOfFragments, String senderOdsCode) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        final String inboundConversationId = UUID.randomUUID().toString().toUpperCase();
        final List<String> fragmentMessageIds = Stream
                .generate(() -> UUID.randomUUID().toString().toUpperCase())
                .limit(numberOfFragments)
                .toList();

        sendLargeEhrCoreWithVariableManifestToRepo(nhsNumber, inboundConversationId, senderOdsCode, fragmentMessageIds);
        generateFragmentsWithoutReferencesForConversationId(senderOdsCode, inboundConversationId, fragmentMessageIds);

        stopWatch.stop();

        log.info("Large EHR with {} fragments added to the repository in {} seconds.", numberOfFragments, (stopWatch.getTime() / 1000));
    }

    // =================== HELPER METHODS ===================
    private void sendLargeEhrCoreWithVariableManifestToRepo(
            String nhsNumber,
            String inboundConversationId,
            String senderOdsCode,
            List<String> fragmentMessageIds
    ) {
        final LargeEhrCoreVariableManifestTemplateContext largeEhrCoreTemplateContext = LargeEhrCoreVariableManifestTemplateContext.builder()
                .inboundConversationId(inboundConversationId)
                .referencedFragmentMessageIds(fragmentMessageIds)
                .nhsNumber(nhsNumber)
                .senderOdsCode(senderOdsCode).build();

        final String largeEhrCoreMessage = this.templatingService.getTemplatedString(LARGE_EHR_CORE_VARIABLE_MANIFEST, largeEhrCoreTemplateContext);

        this.transferTrackerService.save(TransferTrackerRecord.builder()
                .conversationId(inboundConversationId)
                .nhsNumber(nhsNumber)
                .sourceGp(Gp2GpSystem.TPP_PTL_INT.odsCode())
                .nemsMessageId(randomNemsMessageId())
                .state(EHR_REQUEST_SENT.status).build());

        this.mhsInboundQueue.sendMessage(largeEhrCoreMessage, inboundConversationId);
        this.transferTrackerService.waitForStatusMatching(inboundConversationId, LARGE_EHR_CONTINUE_REQUEST_SENT.status);
        log.info("Large EHR Core has reached the EHR Repository successfully, Inbound CID: {}, MID: {}, NHS Number: {}.",
                inboundConversationId,
                largeEhrCoreTemplateContext.getLargeEhrCoreMessageId(),
                nhsNumber);
    }

    private void generateFragmentsWithoutReferencesForConversationId(
            String senderOdsCode,
            String inboundConversationId,
            List<String> fragmentMessageIds
    ) {
        if (!inboundConversationId.equals(inboundConversationId.toUpperCase()))
            inboundConversationId = inboundConversationId.toUpperCase();

        for (int i = 0; i < fragmentMessageIds.size(); i++) {
            final LargeEhrFragmentNoReferencesContext context = LargeEhrFragmentNoReferencesContext.builder()
                    .inboundConversationId(inboundConversationId)
                    .fragmentMessageId(fragmentMessageIds.get(i))
                    .senderOdsCode(senderOdsCode)
                    .build();

            final String fragmentMessage = this.templatingService.getTemplatedString(LARGE_EHR_FRAGMENT_NO_REF_4MB, context);
            this.mhsInboundQueue.sendMessage(fragmentMessage, inboundConversationId);

            log.info("Fragment {} of {} sent to the MHS Inbound Queue, Inbound CID: {}, MID: {}.",
                    (i + 1), fragmentMessageIds.size(), context.getInboundConversationId(), fragmentMessageIds.get(i));
        }

        this.transferTrackerService.waitForStatusMatching(inboundConversationId, EHR_TRANSFER_TO_REPO_COMPLETE.status, 5);
        log.info("All fragments have reached the EHR repository successfully.");
    }
}