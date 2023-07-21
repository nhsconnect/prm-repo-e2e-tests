package uk.nhs.prm.e2etests.exception;

public class UnknownAwsRegionException extends RuntimeException {
    private static final String MESSAGE = "The provided AWS Region was either unknown or invalid.";

    public UnknownAwsRegionException() {
        super(MESSAGE);
    }
}
