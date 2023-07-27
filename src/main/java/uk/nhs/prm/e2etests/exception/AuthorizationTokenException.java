package uk.nhs.prm.e2etests.exception;

public class AuthorizationTokenException extends RuntimeException {
    private static final String MESSAGE = "An exception occurred while interacting with the Authorization Token Generator, details: %s";

    public AuthorizationTokenException(String details) {
        super(String.format(MESSAGE, details));
    }
}
