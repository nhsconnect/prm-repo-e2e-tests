package uk.nhs.prm.e2etests.exception;

public class UtilityException extends RuntimeException {
    private static final String MESSAGE = "An exception occurred within a utility class, details: %s";

    public UtilityException(String details) {
        super(String.format(MESSAGE, details));
    }
}
