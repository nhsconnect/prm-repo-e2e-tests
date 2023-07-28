package uk.nhs.prm.e2etests.exception;

public class ActiveRoleArnException extends RuntimeException {
    private static final String MESSAGE = "An exception occurred while trying to Assume Role, details: %s";

    public ActiveRoleArnException(String details) {
        super(String.format(MESSAGE, details));
    }
}
