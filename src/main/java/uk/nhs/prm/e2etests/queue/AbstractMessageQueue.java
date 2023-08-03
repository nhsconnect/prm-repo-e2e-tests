package uk.nhs.prm.e2etests.queue;

import software.amazon.awssdk.services.sqs.model.PurgeQueueInProgressException;
import uk.nhs.prm.e2etests.model.nems.NemsResolutionMessage;
import uk.nhs.prm.e2etests.utility.QueueHelper;
import uk.nhs.prm.e2etests.service.SqsService;
import uk.nhs.prm.e2etests.model.SqsMessage;
import lombok.extern.log4j.Log4j2;

import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;

@Log4j2
public abstract class AbstractMessageQueue {
    private static final String CHECKING_QUEUE_LOG_MESSAGE = "Checking if message is present on: {}";
    private static final String DELETE_ALL_MESSAGES_LOG_MESSAGE = "Attempting to delete all messages on: {}";
    private static final String MESSAGE_FOUND_LOG_MESSAGE = "The message has been found on: {}";

    protected final SqsService sqsService;
    protected final String queueUri;

    protected AbstractMessageQueue(SqsService sqsService, String queueUri) {
        this.sqsService = sqsService;
        this.queueUri = queueUri;
    }

    public void deleteAllMessages() {
        log.info(DELETE_ALL_MESSAGES_LOG_MESSAGE, this.queueUri);
        try {
            sqsService.deleteAllMessagesFrom(queueUri);
        } catch (PurgeQueueInProgressException exception) {
            // PurgeQueueInProgressException is related to the limitation of sqs allowing only one purge queue every 60 sec.
            // We will ignore this exception and just let the test continue.
            log.info(exception);
        }
    }

    public void deleteMessage(SqsMessage sqsMessage) {
        sqsService.deleteMessageFrom(queueUri, sqsMessage.getMessage());
    }

    public SqsMessage getMessageContaining(String substring) {
        log.info(CHECKING_QUEUE_LOG_MESSAGE, this.queueUri);
        final SqsMessage foundMessage = await().atMost(120, TimeUnit.SECONDS)
                .with()
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .until(() -> findMessageContaining(substring), Optional::isPresent).get();
        log.info(MESSAGE_FOUND_LOG_MESSAGE, this.queueUri);
        return foundMessage;
    }

    public List<SqsMessage> getAllMessageContaining(String substring, int expectedNumberOfMessages) {
        // This method for now only try to get at least 2 messages from the queue. May need renaming or amending
        log.info(CHECKING_QUEUE_LOG_MESSAGE, this.queueUri);
        List<SqsMessage> allMessages = new ArrayList<>();
        await().atMost(2, TimeUnit.MINUTES)
                .with()
                .pollInterval(100, TimeUnit.MILLISECONDS).until(() -> {
                    Optional<SqsMessage> found = findMessageContaining(substring);
                    found.ifPresent(allMessages::add);
                    return (allMessages.size() >= expectedNumberOfMessages);
                }, equalTo(true));

        log.info(MESSAGE_FOUND_LOG_MESSAGE, this.queueUri);
        return allMessages;
    }

    public SqsMessage getMessageContainingAttribute(String attribute, String expectedValue) {
        return getMessageContainingAttribute(attribute, expectedValue, 120, TimeUnit.SECONDS);
    }

    public SqsMessage getMessageContainingAttribute(String attribute, String expectedValue, int timeout, TimeUnit timeUnit) {
        log.info(CHECKING_QUEUE_LOG_MESSAGE, this.queueUri);
        return await().atMost(timeout, timeUnit)
                .with()
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .until(() -> findMessageWithAttribute(attribute, expectedValue), Optional::isPresent).get();
    }

    public boolean hasResolutionMessage(NemsResolutionMessage resolutionMessage) {
        log.info(CHECKING_QUEUE_LOG_MESSAGE, this.queueUri);
        await().atMost(120, TimeUnit.SECONDS)
                .with()
                .pollInterval(2, TimeUnit.SECONDS)
                .until(() -> hasResolutionMessageNow(resolutionMessage), equalTo(true));
        return true;
    }

    public List<SqsMessage> getNextMessages(LocalDateTime timeoutAt) {
        log.info(CHECKING_QUEUE_LOG_MESSAGE, this.queueUri);
        int pollInterval = 5;
        long timeoutSeconds = Math.max(LocalDateTime.now().until(timeoutAt, ChronoUnit.SECONDS), pollInterval + 1);

        return await().atMost(timeoutSeconds, TimeUnit.SECONDS)
                .with()
                .pollInterval(pollInterval, TimeUnit.SECONDS)
                .until(() -> findMessagesOnQueue((int) timeoutSeconds), messages -> !messages.isEmpty());
    }

    private List<SqsMessage> findMessagesOnQueue(int visibilityTimeout) {
        return sqsService.readThroughMessages(this.queueUri, visibilityTimeout);
    }

    private Optional<SqsMessage> findMessageWithCondition(Predicate<SqsMessage> condition) {
        log.info(CHECKING_QUEUE_LOG_MESSAGE, String.format("%s, with a provided condition", this.queueUri));

        List<SqsMessage> allMessages = sqsService.readThroughMessages(this.queueUri, 180);
        return allMessages.stream().filter(condition).findFirst();
    }
    
    private Optional<SqsMessage> findMessageContaining(String substring) {
        return findMessageWithCondition(message -> message.contains(substring));
    }

    public Optional<SqsMessage> findMessageWithAttribute(String attribute, String expectedValue) {
        log.info("Checking if message is present on {} with attribute {}.", this.queueUri, attribute);

        return findMessageWithCondition(message ->
                message.getAttributes().get(attribute).stringValue().equals(expectedValue)
        );
    }

    public Optional<SqsMessage> getMessageContainingForTechnicalTestRun(String substring) {
            log.info(CHECKING_QUEUE_LOG_MESSAGE, this.queueUri);

            return await().atMost(30, TimeUnit.MINUTES)
                    .with()
                    .pollInterval(100, TimeUnit.MILLISECONDS)
                    .until(() -> findMessageContainingWitHigherVisibilityTimeOut(substring), Optional::isPresent);
    }

    private Optional<SqsMessage> findMessageContainingWitHigherVisibilityTimeOut(String substring) {
        final List<SqsMessage> allMessages = sqsService.readThroughMessages(this.queueUri, 600);

        return allMessages.stream()
                .filter(message -> message.contains(substring))
                .findFirst();
    }

    private boolean hasResolutionMessageNow(NemsResolutionMessage messageToCheck) {
        final List<SqsMessage> allMessages = sqsService.readMessagesFrom(this.queueUri);

        return allMessages.stream()
                .map(QueueHelper::mapToNemsResolutionMessage)
                .anyMatch(nemsResolutionMessage -> nemsResolutionMessage.hasTheSameContentAs(messageToCheck));
    }

    protected void postAMessageWithAttributes(String message, Map<String, String> attributes) {
        sqsService.postAMessage(queueUri, message, attributes);
    }
}