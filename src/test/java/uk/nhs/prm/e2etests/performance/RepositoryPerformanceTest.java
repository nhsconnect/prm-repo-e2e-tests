package uk.nhs.prm.e2etests.performance;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.nhs.prm.e2etests.model.MhsMessage;
import uk.nhs.prm.e2etests.model.templatecontext.ContinueRequestTemplateContext;
import uk.nhs.prm.e2etests.model.templatecontext.EhrRequestTemplateContext;
import uk.nhs.prm.e2etests.property.TestConstants;
import uk.nhs.prm.e2etests.queue.SimpleAmqpQueue;
import uk.nhs.prm.e2etests.queue.ehrtransfer.EhrTransferServiceParsingDeadLetterQueue;
import uk.nhs.prm.e2etests.queue.ehrtransfer.observability.EhrTransferServiceLargeEhrFragmentsOQ;
import uk.nhs.prm.e2etests.queue.ehrtransfer.observability.EhrTransferServiceLargeEhrOQ;
import uk.nhs.prm.e2etests.queue.ehrtransfer.observability.EhrTransferServiceNegativeAcknowledgementOQ;
import uk.nhs.prm.e2etests.queue.ehrtransfer.observability.EhrTransferServiceSmallEhrOQ;
import uk.nhs.prm.e2etests.queue.ehrtransfer.observability.EhrTransferServiceTransferCompleteOQ;
import uk.nhs.prm.e2etests.queue.ehrtransfer.observability.EhrTransferServiceUnhandledOQ;
import uk.nhs.prm.e2etests.queue.gp2gpmessenger.observability.Gp2GpMessengerOQ;
import uk.nhs.prm.e2etests.service.RepoService;
import uk.nhs.prm.e2etests.service.TemplatingService;
import uk.nhs.prm.e2etests.test.ForceXercesParserSoLogbackDoesNotBlowUpWhenUsingSwiftMqClient;
import uk.nhs.prm.e2etests.utility.TestDataUtility;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.nhs.prm.e2etests.enumeration.TemplateVariant.CONTINUE_REQUEST;
import static uk.nhs.prm.e2etests.enumeration.TemplateVariant.EHR_REQUEST;
import static uk.nhs.prm.e2etests.property.TestConstants.*;
import static uk.nhs.prm.e2etests.utility.ThreadUtility.sleepFor;

@Log4j2
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(ForceXercesParserSoLogbackDoesNotBlowUpWhenUsingSwiftMqClient.class)
public class RepositoryPerformanceTest {
    private final RepoService repoService;
    private final TemplatingService templatingService;
    private final SimpleAmqpQueue mhsInboundQueue;
    private final Gp2GpMessengerOQ gp2gpMessengerOQ;
    private final EhrTransferServiceTransferCompleteOQ ehrTransferServiceTransferCompleteOQ;
    private final EhrTransferServiceUnhandledOQ ehrTransferServiceUnhandledOQ;
    private final EhrTransferServiceLargeEhrFragmentsOQ ehrTransferServiceLargeEhrFragmentsOQ;
    private final EhrTransferServiceSmallEhrOQ ehrTransferServiceSmallEhrOQ;
    private final EhrTransferServiceLargeEhrOQ ehrTransferServiceLargeEhrOQ;
    private final EhrTransferServiceNegativeAcknowledgementOQ ehrTransferServiceNegativeAcknowledgementOQ;
    private final EhrTransferServiceParsingDeadLetterQueue ehrTransferServiceParsingDeadLetterQueue;

    @Autowired
    public RepositoryPerformanceTest(
            RepoService repoService,
            TemplatingService templatingService,
            SimpleAmqpQueue mhsInboundQueue,
            Gp2GpMessengerOQ gp2gpMessengerOQ,
            EhrTransferServiceTransferCompleteOQ ehrTransferServiceTransferCompleteOQ,
            EhrTransferServiceUnhandledOQ ehrTransferServiceUnhandledOQ,
            EhrTransferServiceLargeEhrFragmentsOQ ehrTransferServiceLargeEhrFragmentsOQ,
            EhrTransferServiceSmallEhrOQ ehrTransferServiceSmallEhrOQ,
            EhrTransferServiceLargeEhrOQ ehrTransferServiceLargeEhrOQ,
            EhrTransferServiceNegativeAcknowledgementOQ ehrTransferServiceNegativeAcknowledgementOQ,
            EhrTransferServiceParsingDeadLetterQueue ehrTransferServiceParsingDeadLetterQueue
    ) {
        this.repoService = repoService;
        this.templatingService = templatingService;
        this.mhsInboundQueue = mhsInboundQueue;
        this.gp2gpMessengerOQ = gp2gpMessengerOQ;
        this.ehrTransferServiceTransferCompleteOQ = ehrTransferServiceTransferCompleteOQ;
        this.ehrTransferServiceUnhandledOQ = ehrTransferServiceUnhandledOQ;
        this.ehrTransferServiceLargeEhrFragmentsOQ = ehrTransferServiceLargeEhrFragmentsOQ;
        this.ehrTransferServiceSmallEhrOQ = ehrTransferServiceSmallEhrOQ;
        this.ehrTransferServiceLargeEhrOQ = ehrTransferServiceLargeEhrOQ;
        this.ehrTransferServiceNegativeAcknowledgementOQ = ehrTransferServiceNegativeAcknowledgementOQ;
        this.ehrTransferServiceParsingDeadLetterQueue = ehrTransferServiceParsingDeadLetterQueue;
    }

    @BeforeAll
    void init() {
        ehrTransferServiceSmallEhrOQ.deleteAllMessages();
        ehrTransferServiceLargeEhrOQ.deleteAllMessages();
        ehrTransferServiceLargeEhrFragmentsOQ.deleteAllMessages();
        ehrTransferServiceParsingDeadLetterQueue.deleteAllMessages();
        ehrTransferServiceTransferCompleteOQ.deleteAllMessages();
        ehrTransferServiceUnhandledOQ.deleteAllMessages();
        ehrTransferServiceNegativeAcknowledgementOQ.deleteAllMessages();
        gp2gpMessengerOQ.deleteAllMessages();
    }

    @BeforeEach
    void beforeEach(TestInfo testInfo) {
        TestConstants.generateTestConstants(testInfo.getDisplayName());
    }

    @Test
    void Given_SuperLargeEhrWith100Fragments_When_PutIntoRepoAndPulledOut_Then_VisibleOnGp2gpMessengerOQ() {
        // Given
        MhsMessage ehrRequest = this.buildEhrRequest(outboundConversationId);
        MhsMessage continueRequest = this.buildContinueRequest(outboundConversationId);

        // When
        this.repoService.addLargeEhrWithVariableManifestToRepo(100);
        this.mhsInboundQueue.sendMessage(ehrRequest.getMessage(), outboundConversationId);

        sleepFor(10000);

        this.mhsInboundQueue.sendMessage(continueRequest.getMessage(), outboundConversationId);

        boolean messagesFound = this.gp2gpMessengerOQ.getAllMessagesFromQueueWithConversationIds(1, 100,
                List.of(outboundConversationId));

        // Then
        assertTrue(messagesFound);
    }

    @Test
    void Given_30LargeEhrsWith5FragmentsEach_When_PutIntoRepoAndPulledOutIndividuallyEveryMinute_Then_VisibleOnGp2gpMessengerOQ() {
        // Constants
        final int numberOfEhrs = 30;
        final int numberOfFragmentsPerEhr = 5;
        final int pullRateMilliseconds = 60000;
        final StopWatch stopWatch = new StopWatch();
        
        // Given
        final List<String> outboundConversationIds = Stream.generate(TestDataUtility::randomUppercaseUuidAsString)
                .limit(numberOfEhrs)
                .toList();

        outboundConversationIds.forEach(conversationId -> {
            stopWatch.start();
            this.repoService.addLargeEhrWithVariableManifestToRepo(numberOfFragmentsPerEhr);
            stopWatch.stop();

            MhsMessage ehrRequest = this.buildEhrRequest(conversationId);
            MhsMessage continueRequest = this.buildContinueRequest(conversationId);

            mhsInboundQueue.sendMessage(ehrRequest.getMessage(), ehrRequest.getConversationId());
            log.info("EHR Request Sent for Outbound Conversation ID: {}.", conversationId);

            sleepFor((pullRateMilliseconds - (int) stopWatch.getTime(TimeUnit.MILLISECONDS)) / 2);

            mhsInboundQueue.sendMessage(continueRequest.getMessage(), continueRequest.getConversationId());
            log.info("Continue Request Sent for Outbound Conversation ID: {}.", conversationId);

            sleepFor((pullRateMilliseconds - (int) stopWatch.getTime(TimeUnit.MILLISECONDS)) / 2);
            stopWatch.reset();
        });

        // then
        final boolean messagesFound = this.gp2gpMessengerOQ.getAllMessagesFromQueueWithConversationIds(numberOfEhrs,
                numberOfEhrs * numberOfFragmentsPerEhr,
                outboundConversationIds);

        assertTrue(messagesFound);
    }

    @Test
    @Timeout(value = 60, unit = SECONDS)
    void shouldTransferOut20EHRsWithin1Minute() {
        List<String> outboundConversationIds = Stream.generate(TestDataUtility::randomUppercaseUuidAsString)
                .limit(20)
                .toList();

        outboundConversationIds.forEach(conversationId -> {
            EhrRequestTemplateContext ehrRequestTemplateContext = EhrRequestTemplateContext.builder()
                    .outboundConversationId(conversationId)
                    .nhsNumber(nhsNumber)
                    .senderOdsCode(senderOdsCode)
                    .asidCode(asidCode)
                    .build();

            String ehrRequestMessage = this.templatingService.getTemplatedString(EHR_REQUEST, ehrRequestTemplateContext);

            this.mhsInboundQueue.sendMessage(ehrRequestMessage, conversationId);
        });

        boolean messagesExist = this.gp2gpMessengerOQ.getAllMessagesFromQueueWithConversationIds(
                20, 0,
                outboundConversationIds
        );

        // then
        assertTrue(messagesExist);
    }

    /**
     * Generates an `EhrRequest` object, which contains the Outbound Conversation ID
     * and the generated message.
     * @param nhsNumber The patient NHS Number.
     * @param senderOdsCode The sending ODS Code.
     * @param asidCode The ASID Code.
     * @return The created `EhrRequest` instance.
     */
    private MhsMessage buildEhrRequest(String outboundConversationId) {
        final EhrRequestTemplateContext context =
                EhrRequestTemplateContext.builder()
                        .nhsNumber(nhsNumber)
                        .outboundConversationId(outboundConversationId)
                        .senderOdsCode(senderOdsCode)
                        .asidCode(asidCode)
                        .build();

        return MhsMessage.builder()
                .conversationId(outboundConversationId)
                .messageId(messageId)
                .message(this.templatingService.getTemplatedString(EHR_REQUEST, context))
                .build();
    }

    /**
     * Generates a `ContinueRequest` object, which contains the generated message.
     * @param outboundConversationId The Outbound Conversation ID.
     * @param recipientOdsCode The Recipient ODS Code.
     * @param senderOdsCode The Sender ODS Code.
     * @return The created `ContinueRequest` instance.
     */
    private MhsMessage buildContinueRequest(String outboundConversationId) {
        final ContinueRequestTemplateContext context = ContinueRequestTemplateContext.builder()
                .outboundConversationId(outboundConversationId)
                .messageId(messageId)
                .recipientOdsCode(recipientOdsCode)
                .senderOdsCode(senderOdsCode).build();

        return MhsMessage.builder()
                .conversationId(outboundConversationId)
                .messageId(messageId)
                .message(this.templatingService.getTemplatedString(CONTINUE_REQUEST, context))
                .build();
    }
}