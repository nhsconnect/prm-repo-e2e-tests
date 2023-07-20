package uk.nhs.prm.e2etests.exception;

public class UnknownRegionException extends RuntimeException {
    private static final String MESSAGE = "The provided AWS Region was either unknown or invalid.";

    public UnknownRegionException() {
        super(MESSAGE);
    }
}
