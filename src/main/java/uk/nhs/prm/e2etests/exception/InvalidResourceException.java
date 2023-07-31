package uk.nhs.prm.e2etests.exception;

public class InvalidResourceException extends RuntimeException {
    private static final String MESSAGE = "An exception occurred while attempting to access a resource, details: %s";

    public InvalidResourceException(String details) {
        super(String.format(MESSAGE, details));
    }
}