package uk.nhs.prm.e2etests.exception;

public class InvalidAmqpEndpointException extends RuntimeException {
    private static final String MESSAGE = "An invalid AMQP endpoint was provided, received: %s";

    public InvalidAmqpEndpointException(String endpoint) {
        super(String.format(MESSAGE, endpoint));
    }
}
