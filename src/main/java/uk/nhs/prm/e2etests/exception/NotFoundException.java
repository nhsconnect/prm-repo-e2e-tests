package uk.nhs.prm.e2etests.exception;

public class NotFoundException extends RuntimeException {
    private static final String MESSAGE = "A record could not be found with ID: %s";

    public NotFoundException(String id) {
        super(String.format(MESSAGE, id));
    }
}
