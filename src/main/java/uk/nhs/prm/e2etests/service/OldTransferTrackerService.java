package uk.nhs.prm.e2etests.service;

import org.springframework.stereotype.Service;
import uk.nhs.prm.e2etests.model.database.OldTransferTrackerRecord;
import uk.nhs.prm.e2etests.repository.OldTransferTrackerDatabaseRepository;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.IsEqual.equalTo;

/**
 * @deprecated PRMT-4670 old Transfer Tracker DB logic - to be removed
 */
@Deprecated(since="2.0.0")
@Service
public class OldTransferTrackerService {

    private final OldTransferTrackerDatabaseRepository oldTransferTrackerDatabaseRepository;

    public OldTransferTrackerService(OldTransferTrackerDatabaseRepository oldTransferTrackerDatabaseRepository) {
        this.oldTransferTrackerDatabaseRepository = oldTransferTrackerDatabaseRepository;
    }

    public boolean conversationIdExists(String conversationId) {
        return oldTransferTrackerDatabaseRepository.findByConversationId(conversationId).isPresent();
    }

    public boolean isStatusForConversationIdPresent(String conversationId, String status) {
        return isStatusForConversationIdPresent(conversationId, status, 120);
    }

    public boolean isStatusForConversationIdPresent(String conversationId, String status, long timeout) {
        await().atMost(timeout, TimeUnit.SECONDS)
                .with()
                .pollInterval(2, TimeUnit.SECONDS)
                .until(() -> oldTransferTrackerDatabaseRepository
                        .findByConversationId(conversationId)
                        .map(OldTransferTrackerRecord::getState)
                        .orElse("entry not found"), equalTo(status));
        return true;
    }

    public String waitForStatusMatching(String conversationId, String partialStatus) {
        return await().atMost(120, TimeUnit.SECONDS)
                .with()
                .pollInterval(2, TimeUnit.SECONDS)
                .until(() -> oldTransferTrackerDatabaseRepository
                        .findByConversationId(conversationId)
                        .map(OldTransferTrackerRecord::getState)
                        .orElse("entry not found"), containsString(partialStatus));
    }

    public String waitForStatusMatching(String conversationId, String partialStatus, int timeoutMinutes) {
        return await().atMost(timeoutMinutes, TimeUnit.MINUTES)
                .with()
                .pollInterval(2, TimeUnit.SECONDS)
                .until(() -> oldTransferTrackerDatabaseRepository
                        .findByConversationId(conversationId)
                        .map(OldTransferTrackerRecord::getState)
                        .orElse("entry not found"), containsString(partialStatus));
    }

    public void save(OldTransferTrackerRecord entry) {
        oldTransferTrackerDatabaseRepository.save(entry);
    }
}
