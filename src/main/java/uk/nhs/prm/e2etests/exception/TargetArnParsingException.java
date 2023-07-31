package uk.nhs.prm.e2etests.exception;

public class TargetArnParsingException extends RuntimeException {
    private static final String MESSAGE = "The target arn could not be parsed, details: %s.";

    public TargetArnParsingException(String details) {
        super(String.format(MESSAGE, details));
    }
}
