package uk.nhs.prm.e2etests.queue;

import software.amazon.awssdk.services.sqs.model.PurgeQueueInProgressException;
import uk.nhs.prm.e2etests.model.nems.NemsResolutionMessage;
import uk.nhs.prm.e2etests.utility.QueueHelper;
import uk.nhs.prm.e2etests.service.SqsService;
import uk.nhs.prm.e2etests.model.SqsMessage;
import lombok.extern.log4j.Log4j2;

import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.notNullValue;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;

@Log4j2
public abstract class AbstractMessageQueue {
    protected final SqsService sqsService;
    protected final String queueUri;

    protected AbstractMessageQueue(SqsService sqsService, String queueUri) {
        this.sqsService = sqsService;
        this.queueUri = queueUri;
    }

    public void deleteAllMessages() {
        log.info("Attempting to delete all the messages on: {}", this.queueUri);
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
        log.info("Checking if message is present on: {}", this.queueUri);
        final SqsMessage foundMessage = await().atMost(120, TimeUnit.SECONDS)
                .with()
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .until(() -> findMessageContaining(substring), notNullValue());
        log.info("The message has been found on: {}", this.queueUri);
        return foundMessage;
    }

    public List<SqsMessage> getAllMessageContaining(String substring) {
        log.info(String.format("Checking if message is present on : %s", this.queueUri));
        List<SqsMessage> allMessages = new ArrayList<>();
        await().atMost(2, TimeUnit.MINUTES)
                .with()
                .pollInterval(100, TimeUnit.MILLISECONDS).until(() -> {
                    SqsMessage found = findMessageContaining(substring);
                    if (found != null) {
                        allMessages.add(found);
                    }
                    return (allMessages.size() >= 2);
                }, equalTo(true));

        log.info(String.format("Found message on : %s", this.queueUri));
        return allMessages;
    }

    public SqsMessage getMessageContainingAttribute(String attribute, String expectedValue) {
        return getMessageContainingAttribute(attribute, expectedValue, 120, TimeUnit.SECONDS);
    }

    public SqsMessage getMessageContainingAttribute(String attribute, String expectedValue, int timeout, TimeUnit timeUnit) {
        log.info(String.format("Checking if message is present on : %s", this.queueUri));
        return await().atMost(timeout, timeUnit)
                .with()
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .until(() -> findMessageWithAttribute(attribute, expectedValue), notNullValue());
    }

    public boolean hasResolutionMessage(NemsResolutionMessage resolutionMessage) {
        log.info(String.format("Checking if message is present on : %s", this.queueUri));
        await().atMost(120, TimeUnit.SECONDS)
                .with()
                .pollInterval(2, TimeUnit.SECONDS)
                .until(() -> hasResolutionMessageNow(resolutionMessage), equalTo(true));
        return true;
    }

    public List<SqsMessage> getNextMessages(LocalDateTime timeoutAt) {
        log.info(String.format("Checking for messages on : %s", this.queueUri));
        int pollInterval = 5;
        long timeoutSeconds = Math.max(LocalDateTime.now().until(timeoutAt, ChronoUnit.SECONDS), pollInterval + 1);
        return await().atMost(timeoutSeconds, TimeUnit.SECONDS)
                .with()
                .pollInterval(pollInterval, TimeUnit.SECONDS)
                .until(() -> findMessagesOnQueue((int) timeoutSeconds), notNullValue());
    }

    private List<SqsMessage> findMessagesOnQueue(int visibilityTimeout) {
        List<SqsMessage> messages = sqsService.readThroughMessages(this.queueUri, visibilityTimeout);
        return messages.isEmpty() ? null : messages;
    }

    private SqsMessage findMessageContaining(String substring) {
        List<SqsMessage>  allMessages = sqsService.readThroughMessages(this.queueUri, 180);
        for (SqsMessage message : allMessages) {
            log.info(String.format("just finding message, checking conversationId: %s", this.queueUri));
            if (message.contains(substring)) {
                return message;
            }
        }
        return null;
    }

    public SqsMessage findMessageWithAttribute(String attribute, String expectedValue) {
        List<SqsMessage> allMessages = sqsService.readThroughMessages(this.queueUri, 180);
        for (SqsMessage message : allMessages) {
            System.out.println("just finding message, checking attribute : " + attribute + " expected value is : " + expectedValue);
            if (message.getAttributes().get(attribute).stringValue().equals(expectedValue)) {
                return message;
            }
        }
        return null;
    }

    public SqsMessage getMessageContainingForTechnicalTestRun(String substring) {
        log.info(String.format("Checking if message is present on : %s", this.queueUri));
        return await().atMost(1800, TimeUnit.SECONDS)
                .with()
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .until(() -> findMessageContainingWitHigherVisibilityTimeOut(substring), notNullValue());
    }

    private SqsMessage findMessageContainingWitHigherVisibilityTimeOut(String substring) {
        final List<SqsMessage> allMessages = sqsService.readThroughMessages(this.queueUri, 600);
        return allMessages.stream()
                .filter(message -> message.contains(substring))
                .findFirst()
                .orElse(null);
    }

    private boolean hasResolutionMessageNow(NemsResolutionMessage messageToCheck) {
        final List<SqsMessage> allMessages = sqsService.readMessagesFrom(this.queueUri);

        return allMessages.stream()
                .map(QueueHelper::mapToNemsResolutionMessage)
                .anyMatch(nemsResolutionMessage -> nemsResolutionMessage.equals(messageToCheck));
    }

    protected void postAMessageWithAttributes(String message, Map<String, String> attributes) {
        sqsService.postAMessage(queueUri, message, attributes);
    }
}
