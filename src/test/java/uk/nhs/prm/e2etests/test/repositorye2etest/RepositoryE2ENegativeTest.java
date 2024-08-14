package uk.nhs.prm.e2etests.test.repositorye2etest;

import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import uk.nhs.prm.e2etests.enumeration.MessageType;
import uk.nhs.prm.e2etests.model.SqsMessage;
import uk.nhs.prm.e2etests.model.database.ConversationRecord;
import uk.nhs.prm.e2etests.model.templatecontext.AcknowledgementTemplateContext;
import uk.nhs.prm.e2etests.model.templatecontext.ContinueRequestTemplateContext;
import uk.nhs.prm.e2etests.model.templatecontext.EhrRequestTemplateContext;
import uk.nhs.prm.e2etests.property.TestConstants;
import uk.nhs.prm.e2etests.queue.SimpleAmqpQueue;
import uk.nhs.prm.e2etests.queue.ehrtransfer.EhrTransferServiceParsingDeadLetterQueue;
import uk.nhs.prm.e2etests.queue.ehrtransfer.observability.*;
import uk.nhs.prm.e2etests.queue.gp2gpmessenger.observability.Gp2GpMessengerOQ;
import uk.nhs.prm.e2etests.service.RepoService;
import uk.nhs.prm.e2etests.service.TemplatingService;
import uk.nhs.prm.e2etests.service.TransferTrackerService;
import uk.nhs.prm.e2etests.test.ForceXercesParserSoLogbackDoesNotBlowUpWhenUsingSwiftMqClient;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.nhs.prm.e2etests.enumeration.ConversationTransferStatus.*;
import static uk.nhs.prm.e2etests.enumeration.Gp2GpSystem.EMIS_PTL_INT;
import static uk.nhs.prm.e2etests.enumeration.Gp2GpSystem.TPP_PTL_INT;
import static uk.nhs.prm.e2etests.enumeration.Patient.PATIENT_WITH_SMALL_EHR_IN_REPO_AND_MOF_SET_TO_TPP;
import static uk.nhs.prm.e2etests.enumeration.Patient.SUSPENDED_WITH_EHR_AT_TPP;
import static uk.nhs.prm.e2etests.enumeration.TemplateVariant.*;
import static uk.nhs.prm.e2etests.property.TestConstants.*;
import static uk.nhs.prm.e2etests.utility.TestDataUtility.randomUppercaseUuidAsString;

@Log4j2
@SpringBootTest
@ExtendWith(ForceXercesParserSoLogbackDoesNotBlowUpWhenUsingSwiftMqClient.class)
@TestPropertySource(properties = {"test.pds.username=e2e-test"})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RepositoryE2ENegativeTest {
    private final TransferTrackerService transferTrackerService;
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
    public RepositoryE2ENegativeTest(
            TransferTrackerService transferTrackerService,
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
        this.transferTrackerService = transferTrackerService;
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

    // TODO: ABSTRACT THIS OUT TO ANOTHER CLASS
    private Arguments erroneousInboundMessage_UnrecognisedInteractionID() {
        final String invalidInteractionId = "TEST_XX123456XX01";
        final String nhsNumber = PATIENT_WITH_SMALL_EHR_IN_REPO_AND_MOF_SET_TO_TPP.nhsNumber();
        log.info("nhsNumber: " + nhsNumber);

        EhrRequestTemplateContext ehrRequestContext = EhrRequestTemplateContext.builder()
                .outboundConversationId(outboundConversationId)
                .nhsNumber(nhsNumber)
                .senderOdsCode(senderOdsCode)
                .build();

        String inboundMessage = this.templatingService.getTemplatedString(EHR_REQUEST, ehrRequestContext);

        String erroneousInboundMessage = inboundMessage
                .replaceAll(MessageType.EHR_REQUEST.interactionId, invalidInteractionId);

        return Arguments.of(
                Named.of("Message with unrecognised Interaction ID", erroneousInboundMessage),
                outboundConversationId
        );
    }

    // TODO: ABSTRACT THIS OUT TO ANOTHER CLASS
    private Arguments erroneousInboundMessage_ContinueRequestWithUnrecognisedConversationId() {
        ContinueRequestTemplateContext continueRequestContext = ContinueRequestTemplateContext.builder()
                .outboundConversationId(outboundConversationId)
                .recipientOdsCode(recipientOdsCode)
                .senderOdsCode(senderOdsCode)
                .build();

        String continueRequestMessage = this.templatingService
                .getTemplatedString(CONTINUE_REQUEST, continueRequestContext);

        return Arguments.of(
                Named.of("Continue Request with unrecognised Conversation ID", continueRequestMessage),
                outboundConversationId
        );
    }

    // TODO: ABSTRACT THIS OUT TO ANOTHER CLASS
    private Stream<Arguments> erroneousInboundMessages() {
        return Stream.of(
                erroneousInboundMessage_UnrecognisedInteractionID(),
                erroneousInboundMessage_ContinueRequestWithUnrecognisedConversationId()
        );
    }

    /**
     * <p>Ensure that received erroneous EHR requests are rejected via the ehr-transfer-service-unhandled-queue.</p>
     * <ul>
     *     <li>Simulate the receipt of an erroneous GP2GP EHR request message via the mhsInboundQueue to be processed at
     *     the ehr-transfer-service.
     *     </li>
     *     <li>Assert that the message is rejected via the ehr-transfer-service-unhandled-queue.</li>
     * </ul>
     */
//    @ParameterizedTest(name = "[Should reject {0}")
//    @MethodSource("erroneousInboundMessages")
//    @DisplayName("Should reject erroneous inbound EHR request messages")
//    void shouldRejectErroneousEhrRequestMessages(String inboundMessage, String conversationId) {
    //FIXME: These parameterized tests seem to be problematic at the moment.
//       The inboundMessage reuse the same conversationId from prev test, rather than using the fresh ones generated at beforeEach
//      log.info("conversationId generated for this test: {}", outboundConversationId);
//      log.info("Actual conversationId being used: {}", conversationId);

//        //Given that we have an erroneous inbound EHR request message
//        // When the message is received via the mhsInboundQueue
//        mhsInboundQueue.sendMessage(inboundMessage, conversationId);
//
//        // Then the ehr-transfer-service will reject the message via the ehr-transfer-service-unhandled-queue
//        SqsMessage unhandledMessage = ehrTransferServiceUnhandledOQ.getMessageContaining(conversationId);
//        assertThat(unhandledMessage.getBody()).isEqualTo(inboundMessage);
//
//        assertTrue(gp2gpMessengerOQ.verifyNoMessageContaining(conversationId));
//
//    }

    /**
     * Ensure that an EHR out request is rejected when the ODS code of the requesting GP is different to the ODS code
     * of the patient as is returned by PDS.
     * <ul>
     *     <li>Add an entry to the transfer tracker db, bypassing the repo-incoming-queue and sending of an EHR
     *     request by the ehr-transfer-service.</li>
     *     <li>Simulate the receipt of a small EHR from an FSS/GP via the mhsInboundQueue and ensure this is
     *     transferred into the repository.</li>
     *     <li>Simulate the receipt of a EHR out request from an FSS/GP via the mhsInboundQueue for which the ODS code differs
     *     to that of the patient.</li>
     *     <li>Assert that the EHR is not sent out to the FSS/GP.</li>
     * </ul>
     */
    @Test
    void shouldRejectEhrOutRequestFromGpWherePatientIsNotRegistered() {
        final String nhsNumber = PATIENT_WITH_SMALL_EHR_IN_REPO_AND_MOF_SET_TO_TPP.nhsNumber();
        final String senderOdsCode = EMIS_PTL_INT.odsCode();
        final String asidCode = TPP_PTL_INT.asidCode();

        log.info("nhsNumber: " + nhsNumber);
        log.info("senderOdsCode: " + senderOdsCode);
        log.info("asidCode: " + asidCode);

        // Given a small EHR exists in the repository
        this.repoService.addSmallEhrToEhrRepo(SMALL_EHR, nhsNumber);

        // When an EHR out request is received from a GP where the patient is not registered (different ODS code)
        EhrRequestTemplateContext templateContext = EhrRequestTemplateContext.builder()
                .outboundConversationId(outboundConversationId)
                .nhsNumber(nhsNumber)
                .senderOdsCode(senderOdsCode)
                .asidCode(asidCode)
                .build();

        String ehrRequestMessage = this.templatingService.getTemplatedString(EHR_REQUEST, templateContext);

        mhsInboundQueue.sendMessage(ehrRequestMessage, outboundConversationId);
        log.info("EHR out request sent successfully");

        // Then the EHR request is rejected

        // Assert that the EHR has not been sent
        assertTrue(gp2gpMessengerOQ.verifyNoMessageContaining(outboundConversationId, 20));
        assertDoesNotThrow(() -> transferTrackerService.waitForFailureReasonMatching(inboundConversationId, "OUTBOUND:incorrect_ods_code"));
        assertDoesNotThrow(() -> transferTrackerService.waitForConversationTransferStatusMatching(inboundConversationId, OUTBOUND_FAILED.name()));
    }

    /**
     * Ensure the correct behaviour on the receipt of a negative acknowledgement from an FSS following an outbound EHR request.
     * <ul>
     *     <li>Add an entry to the transfer tracker db, bypassing the repo-incoming-queue and sending of an EHR
     *     request by the ehr-transfer-service.</li>
     *     <li>Simulate the receipt of a negative acknowledgment message from the requested GP.</li>
     *     <li>Assert the transfer tracker db is updated accordingly.</li>
     *     <li>Assert the negative acknowledgement message is processed accordingly via the expected queues.</li>
     * </ul>
     */
    @Test
    void shouldUpdateTransferTrackerDbStatusAndPublishToTransferCompleteQueueWhenNackReceived() {
        // Given that an EHR request has been sent from the repository to an FSS
        final String NEGATIVE_ACKNOWLEDGEMENT_FAILURE_CODE = "30";
        final String nhsNumber = SUSPENDED_WITH_EHR_AT_TPP.nhsNumber();
        final String senderOdsCode = TPP_PTL_INT.odsCode();

        log.info("nhsNumber: " + nhsNumber);
        log.info("senderOdsCode: " + senderOdsCode);


        // create entry in transfer tracker db with status ACTION:EHR_REQUEST_SENT
        this.transferTrackerService.saveConversation(ConversationRecord.builder()
                .inboundConversationId(inboundConversationId)
                .nemsMessageId(nemsMessageId)
                .nhsNumber(nhsNumber)
                .sourceGp(senderOdsCode)
                .transferStatus(INBOUND_REQUEST_SENT.name())
                .associatedTest(testName)
                .build()
        );

        // When a negative acknowledgement message is received
        String ackMessage = this.templatingService.getTemplatedString(NEGATIVE_ACKNOWLEDGEMENT,
                AcknowledgementTemplateContext.builder()
                        .messageId(messageId)
                        .inboundConversationId(inboundConversationId)
                        .build());

        mhsInboundQueue.sendMessage(ackMessage, inboundConversationId);
        log.info("negative acknowledgement message successfully added for stubbed EHR request with conversationId: {}", inboundConversationId);

        // Then the negative acknowledgement message is processed and the transferTrackerStatus is updated as expected
        assertThat(ehrTransferServiceNegativeAcknowledgementOQ.getMessageContaining(inboundConversationId)).isNotNull();

        assertDoesNotThrow(() -> transferTrackerService.waitForFailureCodeMatching(inboundConversationId, NEGATIVE_ACKNOWLEDGEMENT_FAILURE_CODE));
        assertDoesNotThrow(() -> transferTrackerService.waitForConversationTransferStatusMatching(inboundConversationId, INBOUND_FAILED.name()));
    }

    /**
     * Assert that unexpected message formats received via the mhsInboundQueue are rejected via the
     * ehr-transfer-service parsing dead-letter queue.
     * <ul>
     *     <li>Simulate the receipt of an EHR message of an invalid format via the mhsInboundQueue.</li>
     *     <li>Assert that this is unprocessed and added to the ehr-transfer-service parsing dead-letter queue.</li>
     * </ul>
     */
    @Test
    void shouldSendUnexpectedMessageFormatsThroughToEhrTransferServiceDeadLetterQueue() {
        final List<String> unexpectedMessages = List.of(
                "Hello World!",
                "SELECT * FROM Fragment",
                "<html><body><h1>This is html!</body></html>",
                "100110 111010 001011 101001",
                "{}",
                randomUppercaseUuidAsString()
        );

        unexpectedMessages.forEach(message -> {
            mhsInboundQueue.sendUnexpectedMessage(message);
            assertThat(ehrTransferServiceParsingDeadLetterQueue.getMessageContaining(message)).isNotNull();
        });
    }
}
