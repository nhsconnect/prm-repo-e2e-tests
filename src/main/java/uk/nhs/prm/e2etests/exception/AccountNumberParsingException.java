package uk.nhs.prm.e2etests.exception;

public class AccountNumberParsingException extends RuntimeException {
    private static final String DEFAULT_MESSAGE = "The AWS account number could not be parsed.";

    public AccountNumberParsingException() {
        super(DEFAULT_MESSAGE);
    }
}
