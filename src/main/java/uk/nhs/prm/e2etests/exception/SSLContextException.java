package uk.nhs.prm.e2etests.exception;

public class SSLContextException extends RuntimeException {
    public static final String MESSAGE = "An exception occurred while attempting to generate the SSL context, details: %s";

    public SSLContextException(String details) {
        super(String.format(MESSAGE, details));
    }
}