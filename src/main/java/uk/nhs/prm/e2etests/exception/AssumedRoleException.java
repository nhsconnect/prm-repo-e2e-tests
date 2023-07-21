package uk.nhs.prm.e2etests.exception;

public class AssumedRoleException extends RuntimeException {
    private static final String MESSAGE = "An exception occurred while trying to Assume Role, details: %s";

    public AssumedRoleException(String details) {
        super(String.format(MESSAGE, details));
    }
}
