package uk.nhs.prm.e2etests.service;

import org.springframework.util.StopWatch;
import uk.nhs.prm.e2etests.model.database.ConversationRecord;
import uk.nhs.prm.e2etests.model.templatecontext.LargeEhrCoreVariableManifestTemplateContext;
import uk.nhs.prm.e2etests.model.templatecontext.LargeEhrFragmentNoReferencesContext;
import uk.nhs.prm.e2etests.model.templatecontext.SmallEhrTemplateContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import uk.nhs.prm.e2etests.enumeration.TemplateVariant;
import uk.nhs.prm.e2etests.queue.SimpleAmqpQueue;
import org.springframework.stereotype.Service;
import lombok.extern.log4j.Log4j2;
import lombok.AllArgsConstructor;
import uk.nhs.prm.e2etests.utility.TestDataUtility;

import java.util.stream.Stream;
import java.util.List;

import static uk.nhs.prm.e2etests.enumeration.ConversationTransferStatus.*;
import static uk.nhs.prm.e2etests.enumeration.TemplateVariant.LARGE_EHR_CORE_VARIABLE_MANIFEST;
import static uk.nhs.prm.e2etests.enumeration.TemplateVariant.LARGE_EHR_FRAGMENT_NO_REF_4MB;
import static uk.nhs.prm.e2etests.property.TestConstants.*;

@Log4j2
@Service
@Validated
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class RepoService {
    private final SimpleAmqpQueue mhsInboundQueue;
    private final TemplatingService templatingService;
    private final TransferTrackerService transferTrackerService;

    public void addSmallEhrToEhrRepo(TemplateVariant templateVariant, String nhsNumber) {
        final SmallEhrTemplateContext smallEhrTemplateContext = SmallEhrTemplateContext.builder()
                .inboundConversationId(inboundConversationId)
                .nhsNumber(nhsNumber)
                .build();

        final String smallEhrMessage = this.templatingService.getTemplatedString(templateVariant, smallEhrTemplateContext);

        this.transferTrackerService.saveConversation(ConversationRecord.builder()
                .inboundConversationId(inboundConversationId)
                .nemsMessageId(nemsMessageId)
                .nhsNumber(nhsNumber)
                .sourceGp(senderOdsCode)
                .associatedTest(testName)
                .transferStatus(INBOUND_REQUEST_SENT.name())
                .build());

        this.mhsInboundQueue.sendMessage(smallEhrMessage, inboundConversationId);
        this.transferTrackerService.waitForConversationTransferStatusMatching(inboundConversationId, INBOUND_COMPLETE.name());
        log.info("Small EHR with Conversation ID {} added to the repository successfully.", inboundConversationId);
    }

    public void addLargeEhrWithVariableManifestToRepo(String nhsNumber, int numberOfFragments) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        final List<String> fragmentMessageIds = Stream
                .generate(TestDataUtility::randomUppercaseUuidAsString)
                .limit(numberOfFragments)
                .toList();


        sendLargeEhrCoreWithVariableManifestToRepo(fragmentMessageIds, nhsNumber);
        generateFragmentsWithoutReferencesForConversationId(fragmentMessageIds);

        stopWatch.stop();

        log.info("Large EHR with {} fragments added to the repository in {} seconds.", numberOfFragments, stopWatch.getTotalTimeSeconds());
    }

    // =================== HELPER METHODS ===================
    private void sendLargeEhrCoreWithVariableManifestToRepo(List<String> fragmentMessageIds, String nhsNumber) {
        final LargeEhrCoreVariableManifestTemplateContext largeEhrCoreTemplateContext = LargeEhrCoreVariableManifestTemplateContext.builder()
                .inboundConversationId(inboundConversationId)
                .nhsNumber(nhsNumber)
                .senderOdsCode(senderOdsCode)
                .referencedFragmentMessageIds(fragmentMessageIds)
                .build();

        final String largeEhrCoreMessage = this.templatingService.getTemplatedString(LARGE_EHR_CORE_VARIABLE_MANIFEST, largeEhrCoreTemplateContext);

        this.transferTrackerService.saveConversation(ConversationRecord.builder()
                .inboundConversationId(inboundConversationId)
                .nhsNumber(nhsNumber)
                .sourceGp(senderOdsCode)
                .nemsMessageId(nemsMessageId)
                .associatedTest(testName)
                .transferStatus(INBOUND_REQUEST_SENT.name()).build());

        this.mhsInboundQueue.sendMessage(largeEhrCoreMessage, inboundConversationId);
        this.transferTrackerService.waitForConversationTransferStatusMatching(inboundConversationId, INBOUND_CONTINUE_REQUEST_SENT.name());
        log.info("Large EHR Core has reached the EHR Repository successfully, Inbound CID: {}, MID: {}, NHS Number: {}.",
                inboundConversationId,
                largeEhrCoreTemplateContext.getLargeEhrCoreMessageId(),
                nhsNumber);
    }

    private void generateFragmentsWithoutReferencesForConversationId(List<String> fragmentMessageIds) {
        for (int i = 0; i < fragmentMessageIds.size(); i++) {
            final LargeEhrFragmentNoReferencesContext context = LargeEhrFragmentNoReferencesContext.builder()
                    .inboundConversationId(inboundConversationId)
                    .fragmentMessageId(fragmentMessageIds.get(i))
                    .senderOdsCode(senderOdsCode)
                    .build();

            final String fragmentMessage = this.templatingService.getTemplatedString(LARGE_EHR_FRAGMENT_NO_REF_4MB, context);
            this.mhsInboundQueue.sendMessage(fragmentMessage, inboundConversationId);

            log.info("Fragment {} of {} sent to the MHS Inbound Queue, Inbound CID: {}, MID: {}.",
                    (i + 1), fragmentMessageIds.size(), inboundConversationId, fragmentMessageIds.get(i));
        }

        this.transferTrackerService.waitForConversationTransferStatusMatching(inboundConversationId, INBOUND_COMPLETE.name(), 5);
        log.info("All fragments have reached the EHR repository successfully.");
    }
}