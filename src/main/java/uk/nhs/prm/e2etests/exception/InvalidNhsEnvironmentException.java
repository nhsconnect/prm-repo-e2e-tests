package uk.nhs.prm.e2etests.exception;

public class InvalidNhsEnvironmentException extends RuntimeException {
    private static final String MESSAGE = "The nhs environment was invalid.";

    public InvalidNhsEnvironmentException() {
        super(MESSAGE);
    }
}
