package uk.nhs.prm.e2etests.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.nhs.prm.e2etests.exception.NotFoundException;
import uk.nhs.prm.e2etests.model.database.ConversationRecord;
import uk.nhs.prm.e2etests.repository.ConversationRepository;
import uk.nhs.prm.e2etests.repository.CoreRepository;

import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;

@Service
@RequiredArgsConstructor
public class TransferTrackerService {
    private final ConversationRepository conversationRepository;
    private final CoreRepository coreRepository;

    public void saveConversation(ConversationRecord conversationRecord) {
        conversationRepository.save(conversationRecord);
    }

    public boolean inboundConversationIdExists(String conversationId) {
        return conversationRepository
            .findConversationByInboundConversationId(conversationId)
            .isPresent();
    }

    public String waitForConversationTransferStatusMatching(String inboundConversationId, String conversationTransferStatus) {
        return waitForConversationRecordAttributeMatching(
            inboundConversationId,
            ConversationRecord::getTransferStatus,
            conversationTransferStatus,
            2
        );
    }

    public void waitForConversationTransferStatusMatching(String inboundConversationId, String conversationTransferStatus, int timeoutMinutes) {
        waitForConversationRecordAttributeMatching(
            inboundConversationId,
            ConversationRecord::getTransferStatus,
            conversationTransferStatus,
            timeoutMinutes
        );
    }

    public String waitForFailureReasonMatching(String inboundConversationId, String failureReason) {
        return waitForConversationRecordAttributeMatching(
            inboundConversationId,
            ConversationRecord::getFailureReason,
            failureReason,
            2
        );
    }

    public String waitForFailureCodeMatching(String inboundConversationId, String failureCode) {
        return waitForConversationRecordAttributeMatching(
            inboundConversationId,
            ConversationRecord::getFailureCode,
            failureCode,
            2
        );
    }

    /**
     * Continuously poll the database until either:
     * the return value of the provided getter (from ConversationRecord) matches the valuetoMatch
     * Or the request times out because the value watch never matched
     *
     * @param inboundConversationId the inboundConversationId of the conversation
     * @param getter a getter from the ConversationRecord model
     * @param valueToMatch the value that the getter is expected to return
     * @param timeoutMinutes how many minutes to wait before timing out
     * @return The return value of getter, assuming it was found before timing out
     */
    private String waitForConversationRecordAttributeMatching(
            String inboundConversationId,
            Function<ConversationRecord, String> getter,
            String valueToMatch,
            int timeoutMinutes
    ) {
        return await().atMost(timeoutMinutes, TimeUnit.MINUTES)
            .with()
            .pollInterval(2, TimeUnit.SECONDS)
            .until(() -> conversationRepository
                .findConversationByInboundConversationId(inboundConversationId)
                .map(getter)
                .orElse("entry not found"), equalTo(valueToMatch));
    }

    public boolean verifyConversationContainsOutboundConversationId(String inboundConversationId, String expectedOutboundConversationId) {
        final ConversationRecord record =
            conversationRepository.findConversationByInboundConversationId(inboundConversationId)
                .orElseThrow(() -> new NotFoundException(inboundConversationId));

        return record.getOutboundConversationId().equals(expectedOutboundConversationId);
    }

    public void softDeleteSmallEhr(String inboundConversationId, Instant instant) {
        conversationRepository.softDeleteConversation(inboundConversationId, instant);
        coreRepository.softDeleteCore(inboundConversationId, instant);
    }

    public void clearConversation(String inboundConversationId) {
        conversationRepository.clearConversation(inboundConversationId);
    }

    public void editCoreInboundMessageId(String inboundConversationId) {
        coreRepository.editInboundMessageId(inboundConversationId);
    }

    public void hardDeleteCore(String inboundConversationId) {
        coreRepository.hardDeleteCore(inboundConversationId);
    }

    public void hardDeleteFragment(String inboundConversationId, String fragmentMessageId){
        coreRepository.hardDeleteFragmentWithId(inboundConversationId, fragmentMessageId);
    }
}