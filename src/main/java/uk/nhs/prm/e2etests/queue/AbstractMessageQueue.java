package uk.nhs.prm.e2etests.queue;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.awaitility.core.ConditionTimeoutException;
import software.amazon.awssdk.services.sqs.model.PurgeQueueInProgressException;
import uk.nhs.prm.e2etests.model.nems.NemsResolutionMessage;
import uk.nhs.prm.e2etests.utility.MappingUtility;
import uk.nhs.prm.e2etests.service.SqsService;
import uk.nhs.prm.e2etests.model.SqsMessage;

import java.time.temporal.ChronoUnit;
import java.time.LocalDateTime;
import java.util.*;

import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@Log4j2
public abstract class AbstractMessageQueue {
    private static final String CHECKING_QUEUE_LOG_MESSAGE = "Checking if message is present on: {}";

    private static final String CHECKING_QUEUE_LOG_MESSAGE_WITH_SUBSTRING = CHECKING_QUEUE_LOG_MESSAGE + ", with substring {}";
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
        log.info(CHECKING_QUEUE_LOG_MESSAGE_WITH_SUBSTRING, this.queueUri, substring);
        final SqsMessage foundMessage = await().atMost(120, TimeUnit.SECONDS)
                .with()
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .until(() -> findMessageContaining(substring), Optional::isPresent).get();
        log.info(MESSAGE_FOUND_LOG_MESSAGE, this.queueUri);
        return foundMessage;
    }

    public Set<SqsMessage> getAllMessagesContaining(String substring, int expectedNumberOfMessages) {
        // This method for now only try to get at least 2 messages from the queue. May need renaming or amending
        log.info(CHECKING_QUEUE_LOG_MESSAGE, this.queueUri);

        // we want to ensure every message is unique as we'll be polling several times
        Set<SqsMessage> allMessages = new HashSet<>();

        await().atMost(5, TimeUnit.MINUTES)
                .with()
                .pollInterval(5, TimeUnit.SECONDS)
                .until(() -> {
                    Set<SqsMessage> messagesFoundThisLoop = findAllMessagesContaining(substring);

                    messagesFoundThisLoop.forEach(message -> {
                        log.info("Found a message, details: {}", message);

                        log.info("Message ID is: " + message.getBody().substring(
                                message.getBody().indexOf("\"Message-Id\":\""))
                        );
                    });

                    allMessages.addAll(messagesFoundThisLoop);
                    log.info(allMessages.size());

                    return (allMessages.size() >= expectedNumberOfMessages);
                }, equalTo(true));

        log.info(MESSAGE_FOUND_LOG_MESSAGE, this.queueUri);
        return allMessages;
    }

    public boolean verifyNoMessageContaining(String substring, int secondsToPoll) {
        // Queue the queue repeatedly for {secondsToPoll}, and return true only if a message with given substring NEVER appeared throughout the period.
        // The reason of using awaitility is to allow for the time taken for communication between micro-services (ehr-out, ehr-repo, gp2gp messenger)
        log.info("Verifying that no message on queue {} contains the substring {}", this.queueUri, substring);
        try {
            await().atMost(secondsToPoll, TimeUnit.SECONDS)
                    .with()
                    .pollInterval(100, TimeUnit.MILLISECONDS)
                    .until(() -> findMessageContaining(substring), Optional::isPresent);
            log.info("A message on {} match the given substring {}. Returning false", this.queueUri, substring);
            return false;
        } catch (ConditionTimeoutException error) {
            log.info("Confirmed no message on {} match the given substring {}. Returning true", this.queueUri, substring);
            return true;
        }
    }

    public boolean verifyNoMessageContaining(String substring) {
        // calls the method with the same name, with secondsToPoll preset to 10 seconds.
        return verifyNoMessageContaining(substring, 10);
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
        int timeoutSeconds = (int) Math.max(LocalDateTime.now().until(timeoutAt, ChronoUnit.SECONDS), pollInterval + 1);

        return await().atMost(timeoutSeconds, TimeUnit.SECONDS)
                .with()
                .pollInterval(pollInterval, TimeUnit.SECONDS)
                .until(() -> sqsService.readThroughMessages(this.queueUri, timeoutSeconds),
                        messages -> !messages.isEmpty());
    }

    private Optional<SqsMessage> findMessageWithCondition(Predicate<SqsMessage> condition) {
        log.info(CHECKING_QUEUE_LOG_MESSAGE, this.queueUri);

        List<SqsMessage> allMessages = sqsService.readThroughMessages(this.queueUri, 180);
        return allMessages.stream().filter(condition).findFirst();
    }

    private Optional<SqsMessage> findMessageContaining(String substring) {
        return findMessageWithCondition(message -> message.contains(substring));
    }

    private Set<SqsMessage> findAllMessagesContaining(String substring) {
        log.info(CHECKING_QUEUE_LOG_MESSAGE, this.queueUri);

        return sqsService.readThroughMessages(this.queueUri, 180).stream()
                .filter(message -> message.contains(substring))
                .collect(Collectors.toSet());
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
        final List<SqsMessage> allMessages = this.sqsService.readMessagesFrom(this.queueUri);

        return allMessages.stream()
                .map(MappingUtility::mapToNemsResolutionMessage)
                .anyMatch(nemsResolutionMessage -> nemsResolutionMessage.equals(messageToCheck));
    }

    protected void postAMessageWithAttributes(String message, Map<String, String> attributes) {
        this.sqsService.postAMessage(queueUri, message, attributes);
    }

    public List<SqsMessage> attemptToGetAllMessagesContaining(String substring, int expectedNumberOfMessages, long secondsToPoll) {
        if(expectedNumberOfMessages <= 0 || secondsToPoll <= 0)
            throw new IllegalArgumentException("Expected number of messages or seconds to poll must be greater than 0.");

        log.info("Attempting to get at least {} message(s) with substring {} on queue {}.", expectedNumberOfMessages, substring, this.queueUri);

        List<SqsMessage> allMessages = new ArrayList<>();

        try {
            await().atMost(secondsToPoll, TimeUnit.SECONDS).with().pollInterval(10, TimeUnit.SECONDS).untilAsserted(() -> {
                Optional<SqsMessage> found = findMessageContaining(substring);

                if (found.isPresent()) {
                    boolean isNewMessage = allMessages.stream()
                            .noneMatch(sqsMessage -> sqsMessage.getId().equals(found.get().getId()));
                    if (isNewMessage) allMessages.add(found.get());
                }
                log.info("Messages found" + allMessages.size());

                assertThat("", allMessages.size() >= expectedNumberOfMessages);
            });
        } catch (ConditionTimeoutException exception) {
            log.error("Failed to get at least {} message(s) with substring {} on queue {}, returning all found.", expectedNumberOfMessages, substring, this.queueUri);
        }

        return allMessages;
    }
}