package uk.nhs.prm.e2etests.service;

import org.springframework.stereotype.Service;
import uk.nhs.prm.e2etests.model.ActiveSuspensionsMessage;
import uk.nhs.prm.e2etests.repository.ActiveSuspensionsDatabaseRepository;

@Service
public class ActiveSuspensionsService {
    private final ActiveSuspensionsDatabaseRepository activeSuspensionsDatabaseRepository;

    public ActiveSuspensionsService(
            ActiveSuspensionsDatabaseRepository activeSuspensionsDatabaseRepository
    ) {
        this.activeSuspensionsDatabaseRepository = activeSuspensionsDatabaseRepository;
    }

    public boolean nhsNumberExists(String conversationId) {
        return this.activeSuspensionsDatabaseRepository.queryWithNhsNumber(conversationId) != null;
    }

    public void save(ActiveSuspensionsMessage message) {
        this.activeSuspensionsDatabaseRepository.save(message);
    }
}
