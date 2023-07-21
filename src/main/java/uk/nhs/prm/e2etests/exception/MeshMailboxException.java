package uk.nhs.prm.e2etests.exception;

public class MeshMailboxException extends RuntimeException {
    private static final String MESSAGE = "An exception occurred while interacting with the Mesh Mailbox, details: %s";

    public MeshMailboxException(String details) {
        super(String.format(MESSAGE, details));
    }
}
