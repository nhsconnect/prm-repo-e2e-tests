package uk.nhs.prm.e2etests.service;

import org.springframework.stereotype.Service;
import uk.nhs.prm.e2etests.model.database.TransferTrackerRecord;
import uk.nhs.prm.e2etests.repository.TransferTrackerDatabaseRepository;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.IsEqual.equalTo;

@Service
public class TransferTrackerService {

    private final TransferTrackerDatabaseRepository transferTrackerDatabaseRepository;

    public TransferTrackerService(TransferTrackerDatabaseRepository transferTrackerDatabaseRepository) {
        this.transferTrackerDatabaseRepository = transferTrackerDatabaseRepository;
    }

    public boolean conversationIdExists(String conversationId) {
        return transferTrackerDatabaseRepository.findByConversationId(conversationId).isPresent();
    }

    public boolean isStatusForConversationIdPresent(String conversationId, String status) {
        return isStatusForConversationIdPresent(conversationId, status, 120);
    }

    public boolean isStatusForConversationIdPresent(String conversationId, String status, long timeout) {
        await().atMost(timeout, TimeUnit.SECONDS)
                .with()
                .pollInterval(2, TimeUnit.SECONDS)
                .until(() -> transferTrackerDatabaseRepository
                        .findByConversationId(conversationId)
                        .map(TransferTrackerRecord::getState)
                        .orElse("entry not found"), equalTo(status));
        return true;
    }

    public String waitForStatusMatching(String conversationId, String partialStatus) {
        return await().atMost(120, TimeUnit.SECONDS)
                .with()
                .pollInterval(2, TimeUnit.SECONDS)
                .until(() -> transferTrackerDatabaseRepository
                        .findByConversationId(conversationId)
                        .map(TransferTrackerRecord::getState)
                        .orElse("entry not found"), containsString(partialStatus));
    }

    public void save(TransferTrackerRecord entry) {
        transferTrackerDatabaseRepository.save(entry);
    }
}
