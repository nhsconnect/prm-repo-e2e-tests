package uk.nhs.prm.e2etests.services;

import uk.nhs.prm.e2etests.client.ActiveSuspensionsDbClient;
import uk.nhs.prm.e2etests.model.ActiveSuspensionsMessage;
import org.springframework.stereotype.Component;

// TODO PRMT-3523: THIS IS A SERVICE, @Component -> @Service - PRMT-3488 RENAME TO ActivateSuspensionsService.java
@Component
public class ActiveSuspensionsDB {
    ActiveSuspensionsDbClient activeSuspensionsDbClient;

    public ActiveSuspensionsDB(
            ActiveSuspensionsDbClient activeSuspensionsDbClient
    ) {
        this.activeSuspensionsDbClient = activeSuspensionsDbClient;
    }

    public boolean nhsNumberExists(String conversationId) {
        var response = activeSuspensionsDbClient.queryDbWithNhsNumber(conversationId);
        if (response != null) {
            return true;
        }
        return false;
    }
    public void save(ActiveSuspensionsMessage message)
    {
        activeSuspensionsDbClient.save(message);
    }
}
