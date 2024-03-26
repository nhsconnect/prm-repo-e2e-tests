package uk.nhs.prm.e2etests.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.nhs.prm.e2etests.model.database.ConversationRecord;
import uk.nhs.prm.e2etests.repository.ConversationRepository;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.containsString;

@Service
@RequiredArgsConstructor
public class TransferTrackerService {
    private final ConversationRepository conversationRepository;

    public boolean conversationIdExists(String conversationId) {
        return conversationRepository
                .findByInboundConversationId(conversationId)
                .isPresent();
    }

    public String waitForStatusMatching(String conversationId, String partialStatus) {
        return waitForStatusMatching(conversationId, partialStatus, 2);
    }

    public String waitForStatusMatching(String conversationId, String partialStatus, int timeoutMinutes) {
        return await().atMost(timeoutMinutes, TimeUnit.MINUTES)
                .with()
                .pollInterval(2, TimeUnit.SECONDS)
                .until(() -> conversationRepository
                        .findByInboundConversationId(conversationId)
                        .map(ConversationRecord::getTransferStatus)
                        .orElse("entry not found"), containsString(partialStatus));
    }

    public void save(ConversationRecord conversationRecord) {
        conversationRepository.save(conversationRecord);
    }
}