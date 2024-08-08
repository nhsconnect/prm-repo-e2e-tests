package uk.nhs.prm.e2etests.test.repositorye2etest;

import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import uk.nhs.prm.e2etests.model.templatecontext.EhrRequestTemplateContext;
import uk.nhs.prm.e2etests.property.NhsProperties;
import uk.nhs.prm.e2etests.property.TestConstants;
import uk.nhs.prm.e2etests.queue.SimpleAmqpQueue;
import uk.nhs.prm.e2etests.queue.ehrtransfer.EhrTransferServiceParsingDeadLetterQueue;
import uk.nhs.prm.e2etests.queue.ehrtransfer.observability.*;
import uk.nhs.prm.e2etests.queue.gp2gpmessenger.observability.Gp2GpMessengerOQ;
import uk.nhs.prm.e2etests.service.RepoService;
import uk.nhs.prm.e2etests.service.TemplatingService;
import uk.nhs.prm.e2etests.test.ForceXercesParserSoLogbackDoesNotBlowUpWhenUsingSwiftMqClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.nhs.prm.e2etests.enumeration.Patient.PATIENT_WITH_SMALL_EHR_IN_REPO_AND_MOF_SET_TO_TPP;
import static uk.nhs.prm.e2etests.enumeration.TemplateVariant.EHR_REQUEST;
import static uk.nhs.prm.e2etests.enumeration.TemplateVariant.SMALL_EHR;
import static uk.nhs.prm.e2etests.property.TestConstants.*;
import static uk.nhs.prm.e2etests.property.TestConstants.asidCode;
import static uk.nhs.prm.e2etests.utility.TestDataUtility.randomUppercaseUuidAsString;

@Log4j2
@SpringBootTest
@ExtendWith(ForceXercesParserSoLogbackDoesNotBlowUpWhenUsingSwiftMqClient.class)
@TestPropertySource(properties = {"test.pds.username=e2e-test"})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RepositoryDuplicateMessageTests {
    private final RepoService repoService;
    private final TemplatingService templatingService;
    private final NhsProperties nhsProperties;
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
    public RepositoryDuplicateMessageTests(
            RepoService repoService,
            TemplatingService templatingService,
            NhsProperties nhsProperties,
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
        this.nhsProperties = nhsProperties;
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

    /**
     * Ensures that only one EHR is sent when multiple EHR out requests are received from the same GP and use the same
     * ConversationId.
     * <ul>
     *      <li>Add a small EHR to the repository.</li>
     *      <li>Simulate the receipt of 2 identical EHR out requests from the same GP and using the same ConversationId.</li>
     *      <li>Assert that only one EHR is transferred to the requesting practice.</li>
     * </ul>
     *
     */
    @Test
    void shouldRejectADuplicateEhrRequestFromTheSameGPWithSameConversationId() {
        final String nhsNumber = PATIENT_WITH_SMALL_EHR_IN_REPO_AND_MOF_SET_TO_TPP.nhsNumber();
        log.info("nhsNumber: " + nhsNumber);

        // Given a small EHR exists in the repository
        repoService.addSmallEhrToEhrRepo(SMALL_EHR, nhsNumber);

        // When 2 identical EHR out requests are received (from same GP, using same ConversationId)

        // Construct an EHR request message
        EhrRequestTemplateContext templateContext = EhrRequestTemplateContext.builder()
                .outboundConversationId(outboundConversationId)
                .nhsNumber(nhsNumber)
                .senderOdsCode(senderOdsCode)
                .asidCode(asidCode)
                .build();
        String ehrRequestMessage = this.templatingService.getTemplatedString(EHR_REQUEST, templateContext);

        // Send 2 EHR out requests
        for (int i = 0; i < 2; i++) {
            mhsInboundQueue.sendMessage(ehrRequestMessage, outboundConversationId);
            log.info("Duplicate EHR Request {} of {} sent to MHS Inbound queue successfully.", (i + 1), 2);
        }

        log.info("Added duplicate EHR out requests to mhsInboundQueue");

        // Then only one EHR is transferred to the requesting practice
        boolean messagesFound = this.gp2gpMessengerOQ.getAllMessagesFromQueueWithConversationIds(1, 0,
                List.of(outboundConversationId));
        assertTrue(messagesFound);
    }

    /**
     * Ensure that only one EHR is sent when multiple EHR out requests are received from the same GP and use different
     * ConversationIds.
     * <ul>
     *      <li>Add a small EHR to the repository.</li>
     *      <li>Simulate the receipt of 2 identical EHR out requests from the same GP and using different ConversationIds.</li>
     *      <li>Assert that only one EHR is transferred to the requesting practice.</li>
     * </ul>
     *
     */
    @Test
    void shouldRejectADuplicateEhrRequestFromTheSameGPWithDifferentConversationId() {
        final String nhsNumber = PATIENT_WITH_SMALL_EHR_IN_REPO_AND_MOF_SET_TO_TPP.nhsNumber();
        log.info("nhsNumber: " + nhsNumber);

        // Given a small EHR exists in the repository
        String outboundConversationId1 = randomUppercaseUuidAsString();
        String outboundConversationId2 = randomUppercaseUuidAsString();

        log.info("Ignore 'outboundConversationId', this test uses outboundConversationId1 and outboundConversationId2");
        log.info("outboundConversationId1: " + outboundConversationId1);
        log.info("outboundConversationId2: " + outboundConversationId2);

        repoService.addSmallEhrToEhrRepo(SMALL_EHR, nhsNumber);

        // When 2 identical EHR out requests are received (from same GP, using same ConversationId)

        // Construct an EHR request message
        EhrRequestTemplateContext templateContext1 = EhrRequestTemplateContext.builder()
                .outboundConversationId(outboundConversationId1)
                .nhsNumber(nhsNumber)
                .senderOdsCode(senderOdsCode)
                .asidCode(asidCode)
                .build();

        String ehrRequestMessage1 = this.templatingService.getTemplatedString(EHR_REQUEST, templateContext1);

        // Construct an EHR request message
        EhrRequestTemplateContext templateContext2 = EhrRequestTemplateContext.builder()
                .outboundConversationId(outboundConversationId2)
                .nhsNumber(nhsNumber)
                .senderOdsCode(senderOdsCode)
                .asidCode(asidCode)
                .build();

        String ehrRequestMessage2 = this.templatingService.getTemplatedString(EHR_REQUEST, templateContext2);

        // Send 2 EHR out requests
        mhsInboundQueue.sendMessage(ehrRequestMessage1, outboundConversationId1);
        mhsInboundQueue.sendMessage(ehrRequestMessage2, outboundConversationId2);

        log.info("added EHR out requests to mhsInboundQueue");

        // Then only one EHR is transferred to the requesting practice
        boolean messagesFound = this.gp2gpMessengerOQ.getAllMessagesFromQueueWithConversationIds(1, 0,
                List.of(outboundConversationId1, outboundConversationId2));
        assertTrue(messagesFound);
    }
}
