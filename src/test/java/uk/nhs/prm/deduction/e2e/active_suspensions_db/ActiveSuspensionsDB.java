package uk.nhs.prm.deduction.e2e.active_suspensions_db;

import uk.nhs.prm.deduction.e2e.reregistration.active_suspensions_db.ActiveSuspensionsDbClient;
import uk.nhs.prm.deduction.e2e.reregistration.models.ActiveSuspensionsMessage;

public class ActiveSuspensionsDB {
    ActiveSuspensionsDbClient activeSuspensionsDbClient;

    public ActiveSuspensionsDB(ActiveSuspensionsDbClient activeSuspensionsDbClient) {
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
