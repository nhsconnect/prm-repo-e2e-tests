package uk.nhs.prm.e2etests.performance;

import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.nhs.prm.e2etests.configuration.TestData;
import uk.nhs.prm.e2etests.enumeration.Gp2GpSystem;
import uk.nhs.prm.e2etests.model.RepoIncomingMessage;
import uk.nhs.prm.e2etests.model.RepoIncomingMessageBuilder;
import uk.nhs.prm.e2etests.model.SqsMessage;
import uk.nhs.prm.e2etests.performance.reporting.RepoInPerformanceChartGenerator;
import uk.nhs.prm.e2etests.queue.SimpleAmqpQueue;
import uk.nhs.prm.e2etests.queue.ehrtransfer.EhrTransferServiceRepoIncomingQueue;
import uk.nhs.prm.e2etests.queue.ehrtransfer.observability.EhrTransferServiceTransferCompleteOQ;
import uk.nhs.prm.e2etests.test.ForceXercesParserSoLogbackDoesNotBlowUpWhenUsingSwiftMqClient;
import uk.nhs.prm.e2etests.utility.ResourceUtility;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static java.lang.Integer.parseInt;
import static java.lang.System.getenv;
import static java.time.LocalDateTime.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.nhs.prm.e2etests.utility.ThreadUtility.sleepFor;

@Log4j2
@SpringBootTest
@ExtendWith(ForceXercesParserSoLogbackDoesNotBlowUpWhenUsingSwiftMqClient.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RepoInPerformanceTest {

    private final EhrTransferServiceRepoIncomingQueue ehrTransferServiceRepoIncomingQueue;
    private final SimpleAmqpQueue inboundQueueFromMhs;
    private final EhrTransferServiceTransferCompleteOQ ehrTransferServiceTransferCompleteOQ;

    @Autowired
    public RepoInPerformanceTest(
            EhrTransferServiceRepoIncomingQueue ehrTransferServiceRepoIncomingQueue,
            SimpleAmqpQueue inboundQueueFromMhs,
            EhrTransferServiceTransferCompleteOQ ehrTransferServiceCompleteOQ
    ) {
        this.ehrTransferServiceRepoIncomingQueue = ehrTransferServiceRepoIncomingQueue;
        this.inboundQueueFromMhs = inboundQueueFromMhs;
        this.ehrTransferServiceTransferCompleteOQ = ehrTransferServiceCompleteOQ;
    }

    @Test
    void trackBehaviourOfHighNumberOfMessagesSentToEhrTransferService() {
        log.info("Starting problematic test.");
        int numberOfMessagesToBeProcessed = getNumberOfMessagesToBeProcessed();
        List<RepoInPerfMessageWrapper> messagesToBeProcessed = setupMessagesToBeProcessed(numberOfMessagesToBeProcessed);

        log.info("The messages have been pre-processed successfully, about to send to message queue.");
        sendMessagesToMq(messagesToBeProcessed);

        log.info("All of the messages have been sent, total: {}. Ensuring they exist on the transfer complete queue.", numberOfMessagesToBeProcessed);
        assertMessagesAreInTransferCompleteQueue(numberOfMessagesToBeProcessed, messagesToBeProcessed);
    }

    private void assertMessagesAreInTransferCompleteQueue(int numberOfMessagesToBeProcessed, List<RepoInPerfMessageWrapper> messagesToBeProcessed) {
        int messagesReadFromQueueEveryMinute = 100;
        int additionalMinutesBuffer = 5;
        int timeoutInMinutes = Math.round(numberOfMessagesToBeProcessed / messagesReadFromQueueEveryMinute) + additionalMinutesBuffer;

        log.info("Polling messages from transfer complete queue, the timeout for this operation has been set to {} minutes.", timeoutInMinutes);

        List<RepoInPerfMessageWrapper> messagesProcessed = new ArrayList<>();
        LocalDateTime timeout = now().plusMinutes(timeoutInMinutes);

        while (now().isBefore(timeout) && !messagesToBeProcessed.isEmpty()) {

            for (SqsMessage sqsMessage : ehrTransferServiceTransferCompleteOQ.getNextMessages(timeout)) {
                String conversationId = sqsMessage.getAttributes().get("conversationId").stringValue();
                messagesToBeProcessed.removeIf(message -> {
                    if (message.getMessage().getConversationId().equals(conversationId)) {
                        message.finish(sqsMessage.getQueuedAt());
                        ehrTransferServiceTransferCompleteOQ.deleteMessage(sqsMessage);
                        log.info("Message found on transfer complete queue with Conversation ID: {}, which took {} seconds to be processed.",
                                conversationId,
                                message.getProcessingTimeInSeconds());
                        messagesProcessed.add(message);
                        return true;
                    }
                    return false;
                });

                int numberOfMessagesProcessed = numberOfMessagesToBeProcessed - messagesToBeProcessed.size();
                log.info("Processed {} of {} messages.", numberOfMessagesProcessed, numberOfMessagesToBeProcessed);
            }
        }

        RepoInPerformanceChartGenerator.generateThroughputPlot(
                messagesProcessed.stream()
                        .sorted(Comparator.comparing(RepoInPerfMessageWrapper::getFinishedAt))
                        .collect(Collectors.toList()));

        assertTrue(messagesToBeProcessed.isEmpty());
    }

    private void sendMessagesToMq(List<RepoInPerfMessageWrapper> messagesToBeProcessed) {
        int intervalBetweenMessagesSentToMq = getIntervalBetweenMessagesSentToMq();
        try {
            String messageTemplate = ResourceUtility.readTestResourceFileFromEhrDirectory("small-ehr-4MB");
            AtomicInteger counter = new AtomicInteger(0);
            String smallEhr;

            for (int i = 0; i < messagesToBeProcessed.size(); i++) {
                counter.updateAndGet(v -> v + 1);
                String conversationId = messagesToBeProcessed.get(i).getMessage().getConversationId();
                smallEhr = getSmallMessageWithUniqueConversationIdAndMessageId(messageTemplate, conversationId);
                messagesToBeProcessed.get(i).start();
                inboundQueueFromMhs.sendMessage(smallEhr, conversationId);
                sleepFor(intervalBetweenMessagesSentToMq);
            }

            log.info("All the messages have been sent, about to close MHS inbound queue producer.");
            inboundQueueFromMhs.close();
        } catch (OutOfMemoryError error) {
            log.fatal("The SwiftMQ client has run out of memory, details: {} - cause: {}.", error.getMessage(), error.getCause());
            System.exit(1);
        }
    }


    private synchronized void processMessage(
            RepoInPerfMessageWrapper message,
            String messageTemplate,
            AtomicInteger counter,
            SimpleAmqpQueue inboundQueueFromMhs
    ) {
        counter.incrementAndGet();
        String conversationId = message.getMessage().getConversationId();

        String smallEhr = getSmallMessageWithUniqueConversationIdAndMessageId(messageTemplate, conversationId);
        message.start();

        log.info("[ITEM #{}] [CONVERSATION ID: {}] - Sending to message queue.", counter.get(), conversationId);
        inboundQueueFromMhs.sendMessage(smallEhr, conversationId);

        sleepFor(100);
        message.finish(LocalDateTime.now());
    }

    private List<RepoInPerfMessageWrapper> setupMessagesToBeProcessed(int numberOfMessagesToBeProcessed) {
        List<RepoInPerfMessageWrapper> messagesToBeProcessed = new ArrayList<>();

        for (int i = 0; i < numberOfMessagesToBeProcessed; i++) {
            RepoIncomingMessage message = new RepoIncomingMessageBuilder()
                    .withNhsNumber(TestData.generateRandomNhsNumber())
                    .withEhrSourceGp(Gp2GpSystem.EMIS_PTL_INT)
                    .build();
            messagesToBeProcessed.add(new RepoInPerfMessageWrapper(message));
        }

        messagesToBeProcessed.forEach(message -> ehrTransferServiceRepoIncomingQueue.send(message.getMessage()));
        return messagesToBeProcessed;
    }

    private int getNumberOfMessagesToBeProcessed() {
        String result = getenv("NUMBER_OF_MESSAGES_TO_BE_PROCESSED");
        return result == null ? 500 : parseInt(result);
    }

    private int getIntervalBetweenMessagesSentToMq() {
        String result = getenv("INTERVAL_BETWEEN_MESSAGES_SENT_TO_MQ");
        return result == null ? 100 : parseInt(result);
    }

    private String getSmallMessageWithUniqueConversationIdAndMessageId(String message, String conversationId) {
        String messageId = randomUUID().toString();
        message = message.replaceAll("__CONVERSATION_ID__", conversationId);
        message = message.replaceAll("__MESSAGE_ID__", messageId);
        return message;
    }
}