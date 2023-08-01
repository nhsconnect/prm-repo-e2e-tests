package uk.nhs.prm.e2etests.exception;

public class InvalidPdsAdaptorUsernameException extends RuntimeException {
    private static final String MESSAGE = "The given PDS Adaptor username was invalid, details: %s";

    public InvalidPdsAdaptorUsernameException(String details) {
        super(String.format(MESSAGE, details));
    }
}