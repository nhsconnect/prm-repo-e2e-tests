package uk.nhs.prm.e2etests.service;

import uk.nhs.prm.e2etests.repository.ActiveSuspensionsDatabaseRepository;
import uk.nhs.prm.e2etests.model.database.ActiveSuspensionsRecord;
import org.springframework.stereotype.Service;

@Service
public class ActiveSuspensionsService {
    private final ActiveSuspensionsDatabaseRepository activeSuspensionsDatabaseRepository;

    public ActiveSuspensionsService(
            ActiveSuspensionsDatabaseRepository activeSuspensionsDatabaseRepository
    ) {
        this.activeSuspensionsDatabaseRepository = activeSuspensionsDatabaseRepository;
    }

    public boolean nhsNumberExists(String nhsNumber) {
        return this.activeSuspensionsDatabaseRepository.findByNhsNumber(nhsNumber).isPresent();
    }

    public void save(ActiveSuspensionsRecord message) {
        this.activeSuspensionsDatabaseRepository.save(message);
    }
}
