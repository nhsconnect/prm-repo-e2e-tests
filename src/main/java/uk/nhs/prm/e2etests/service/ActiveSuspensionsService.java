package uk.nhs.prm.e2etests.service;

import org.springframework.stereotype.Service;
import uk.nhs.prm.e2etests.repository.ActiveSuspensionsDatabaseRepository;
import uk.nhs.prm.e2etests.model.ActiveSuspensionsMessage;

@Service
public class ActiveSuspensionsService {
    private final ActiveSuspensionsDatabaseRepository activeSuspensionsDatabaseRepository;

    public ActiveSuspensionsService(
            ActiveSuspensionsDatabaseRepository activeSuspensionsDatabaseRepository
    ) {
        this.activeSuspensionsDatabaseRepository = activeSuspensionsDatabaseRepository;
    }

    public boolean nhsNumberExists(String conversationId) {
        var response = activeSuspensionsDatabaseRepository.queryWithNhsNumber(conversationId);
        if (response != null) {
            return true;
        }
        return false;
    }
    public void save(ActiveSuspensionsMessage message)
    {
        activeSuspensionsDatabaseRepository.save(message);
    }
}
