package uk.nhs.prm.e2etests.exception;

public class ServiceException extends RuntimeException {
    public static final String MESSAGE = "An exception occurred while interacting with the %s service, details: %s";

    public ServiceException(String className, String details) {
        super(String.format(MESSAGE, className, details));
    }
}