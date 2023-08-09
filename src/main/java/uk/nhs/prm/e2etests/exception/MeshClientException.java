package uk.nhs.prm.e2etests.exception;

public class MeshClientException extends RuntimeException {
    private static final String MESSAGE = "An exception occurred while interacting with the Mesh Mailbox, details: %s";

    public MeshClientException(String details) {
        super(String.format(MESSAGE, details));
    }
}
