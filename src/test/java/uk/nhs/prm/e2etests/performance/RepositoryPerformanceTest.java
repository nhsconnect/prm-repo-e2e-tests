package uk.nhs.prm.e2etests.performance;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.nhs.prm.e2etests.enumeration.Gp2GpSystem;
import uk.nhs.prm.e2etests.model.MhsMessage;
import uk.nhs.prm.e2etests.model.templatecontext.ContinueRequestTemplateContext;
import uk.nhs.prm.e2etests.model.templatecontext.EhrRequestTemplateContext;
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

import static org.junit.jupiter.api.Assertions.*;
import static uk.nhs.prm.e2etests.enumeration.Gp2GpSystem.REPO_DEV;
import static uk.nhs.prm.e2etests.enumeration.Gp2GpSystem.TPP_PTL_INT;
import static uk.nhs.prm.e2etests.enumeration.TemplateVariant.CONTINUE_REQUEST;
import static uk.nhs.prm.e2etests.enumeration.TemplateVariant.EHR_REQUEST;
import static uk.nhs.prm.e2etests.utility.TestDataUtility.randomUuidAsString;
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

    @Test
    void Given_SuperLargeEhrWith100Fragments_When_PutIntoRepoAndPulledOut_Then_VisibleOnGp2gpMessengerOQ() {
        // Constants
        final String nhsNumber = "9727018157";

        // Given
        String outboundConversationId = randomUuidAsString();
        MhsMessage ehrRequest = this.buildEhrRequest(nhsNumber, outboundConversationId, TPP_PTL_INT.odsCode(), TPP_PTL_INT.asidCode());
        MhsMessage continueRequest = this.buildContinueRequest(outboundConversationId, REPO_DEV.odsCode(), TPP_PTL_INT.odsCode());

        // When
        this.repoService.addLargeEhrWithVariableManifestToRepo(nhsNumber, 100, TPP_PTL_INT.odsCode());
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
        final String nhsNumber = "9727018157";
        final int pullRateMilliseconds = 60000;
        final StopWatch stopWatch = new StopWatch();
        
        // Given
        final List<String> outboundConversationIds = Stream.generate(TestDataUtility::randomUuidAsString)
                .limit(numberOfEhrs)
                .toList();

        outboundConversationIds.forEach(conversationId -> {
            stopWatch.start();
            this.repoService.addLargeEhrWithVariableManifestToRepo(nhsNumber, numberOfFragmentsPerEhr, TPP_PTL_INT.odsCode());
            stopWatch.stop();

            MhsMessage ehrRequest = this.buildEhrRequest(nhsNumber, conversationId, TPP_PTL_INT.odsCode(), TPP_PTL_INT.asidCode());
            MhsMessage continueRequest = this.buildContinueRequest(conversationId, Gp2GpSystem.REPO_DEV.odsCode(), TPP_PTL_INT.odsCode());

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

    /**
     * Generates an `EhrRequest` object, which contains the Outbound Conversation ID
     * and the generated message.
     * @param nhsNumber The patient NHS Number.
     * @param sendingOdsCode The sending ODS Code.
     * @param asidCode The ASID Code.
     * @return The created `EhrRequest` instance.
     */
    private MhsMessage buildEhrRequest(String nhsNumber,
                                          String outboundConversationId,
                                          String sendingOdsCode,
                                          String asidCode) {
        final EhrRequestTemplateContext context =
                EhrRequestTemplateContext.builder()
                        .nhsNumber(nhsNumber)
                        .outboundConversationId(outboundConversationId)
                        .sendingOdsCode(sendingOdsCode)
                        .asidCode(asidCode).build();

        log.info("Generated Outbound CID: {}.", context.getOutboundConversationId());

        return MhsMessage.builder()
                .conversationId(context.getOutboundConversationId())
                .messageId(context.getMessageId())
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
    private MhsMessage buildContinueRequest(String outboundConversationId,
                                                    String recipientOdsCode,
                                                    String senderOdsCode) {
        final ContinueRequestTemplateContext context =
                ContinueRequestTemplateContext.builder()
                .outboundConversationId(outboundConversationId)
                .recipientOdsCode(recipientOdsCode)
                .senderOdsCode(senderOdsCode).build();

        return MhsMessage.builder()
                .conversationId(context.getOutboundConversationId())
                .messageId(context.getMessageId())
                .message(this.templatingService.getTemplatedString(CONTINUE_REQUEST, context))
                .build();
    }
}