package uk.nhs.prm.e2etests.exception;

public class GenericException extends RuntimeException {
    private static final String MESSAGE = "An exception occurred within the %s class, details: %s";

    public GenericException(String className, String details) {
        super(String.format(MESSAGE, className, details));
    }
}
