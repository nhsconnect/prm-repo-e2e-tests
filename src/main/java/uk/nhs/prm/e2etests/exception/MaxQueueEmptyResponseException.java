package uk.nhs.prm.e2etests.exception;

public class MaxQueueEmptyResponseException extends RuntimeException {
    private static final String MESSAGE = "The maximum number of empty responses from the queue has been reached.";

    public MaxQueueEmptyResponseException() {
        super(MESSAGE);
    }
}