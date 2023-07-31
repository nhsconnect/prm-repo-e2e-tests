package uk.nhs.prm.e2etests.exception;

public class AccountNumberParsingException extends RuntimeException {
    private static final String DEFAULT_MESSAGE = "The AWS account number could not be parsed.";
    private static final String MESSAGE_WITH_DETAIL = "The AWS account number could not be parsed, details: %s.";

    public AccountNumberParsingException(String details) {
        super(String.format(MESSAGE_WITH_DETAIL, details));
    }

    public AccountNumberParsingException() {
        super(DEFAULT_MESSAGE);
    }
}
