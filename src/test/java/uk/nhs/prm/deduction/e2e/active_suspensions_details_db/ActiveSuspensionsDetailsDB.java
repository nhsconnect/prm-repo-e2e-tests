package uk.nhs.prm.deduction.e2e.active_suspensions_details_db;

public class ActiveSuspensionsDetailsDB {
    DbClient dbClient;

    public ActiveSuspensionsDetailsDB(DbClient dbClient) {
        this.dbClient = dbClient;
    }

    public boolean nhsNumberExists(String conversationId) {
        var response = dbClient.queryDbWithNhsNumber(conversationId);
        if (response != null) {
            return true;
        }
        return false;
    }
}
