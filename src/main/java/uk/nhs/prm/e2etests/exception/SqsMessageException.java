package uk.nhs.prm.e2etests.exception;

public class SqsMessageException extends RuntimeException {
    private static final String MESSAGE = "An exception occurred while interacting with an AWS SQS Queue, details: %s";

    public SqsMessageException(String details) {
        super(String.format(MESSAGE, details));
    }
}
