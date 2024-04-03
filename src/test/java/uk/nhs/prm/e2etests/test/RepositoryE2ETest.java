package uk.nhs.prm.e2etests.test;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.xmlunit.diff.Diff;

import uk.nhs.prm.e2etests.enumeration.Gp2GpSystem;
import uk.nhs.prm.e2etests.enumeration.MessageType;
import uk.nhs.prm.e2etests.enumeration.OldTransferTrackerStatus;
import uk.nhs.prm.e2etests.enumeration.Patient;
import uk.nhs.prm.e2etests.enumeration.TemplateVariant;
import uk.nhs.prm.e2etests.model.SqsMessage;
import uk.nhs.prm.e2etests.model.database.ConversationRecord;
import uk.nhs.prm.e2etests.model.templatecontext.AcknowledgementTemplateContext;
import uk.nhs.prm.e2etests.model.templatecontext.ContinueRequestTemplateContext;
import uk.nhs.prm.e2etests.model.templatecontext.EhrRequestTemplateContext;
import uk.nhs.prm.e2etests.model.templatecontext.LargeEhrCoreTemplateContext;
import uk.nhs.prm.e2etests.model.templatecontext.LargeEhrFragmentNoReferencesContext;
import uk.nhs.prm.e2etests.model.templatecontext.LargeEhrFragmentWithReferencesContext;
import uk.nhs.prm.e2etests.model.templatecontext.SmallEhrTemplateContext;
import uk.nhs.prm.e2etests.property.Gp2gpMessengerProperties;
import uk.nhs.prm.e2etests.property.NhsProperties;
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
import uk.nhs.prm.e2etests.service.TransferTrackerService;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.util.AssertionErrors.assertFalse;

import static uk.nhs.prm.e2etests.enumeration.ConversationTransferStatus.INBOUND_COMPLETE;
import static uk.nhs.prm.e2etests.enumeration.ConversationTransferStatus.INBOUND_CONTINUE_REQUEST_SENT;
import static uk.nhs.prm.e2etests.enumeration.ConversationTransferStatus.INBOUND_REQUEST_SENT;
import static uk.nhs.prm.e2etests.enumeration.Gp2GpSystem.EMIS_PTL_INT;
import static uk.nhs.prm.e2etests.enumeration.Gp2GpSystem.TPP_PTL_INT;
import static uk.nhs.prm.e2etests.enumeration.MessageType.EHR_CORE;
import static uk.nhs.prm.e2etests.enumeration.MessageType.EHR_FRAGMENT;
import static uk.nhs.prm.e2etests.enumeration.TemplateVariant.CONTINUE_REQUEST;
import static uk.nhs.prm.e2etests.enumeration.TemplateVariant.EHR_REQUEST;
import static uk.nhs.prm.e2etests.enumeration.TemplateVariant.LARGE_EHR_CORE;
import static uk.nhs.prm.e2etests.enumeration.TemplateVariant.LARGE_EHR_FRAGMENT_NO_REF;
import static uk.nhs.prm.e2etests.enumeration.TemplateVariant.LARGE_EHR_FRAGMENT_WITH_REF;
import static uk.nhs.prm.e2etests.enumeration.TemplateVariant.NEGATIVE_ACKNOWLEDGEMENT;
import static uk.nhs.prm.e2etests.enumeration.TemplateVariant.SMALL_EHR;
import static uk.nhs.prm.e2etests.enumeration.TemplateVariant.SMALL_EHR_WITH_99_ATTACHMENTS;
import static uk.nhs.prm.e2etests.utility.XmlComparisonUtility.comparePayloads;
import static uk.nhs.prm.e2etests.utility.XmlComparisonUtility.getPayloadOptional;
import static uk.nhs.prm.e2etests.utility.TestDataUtility.randomNemsMessageId;

@Log4j2
@SpringBootTest
@ExtendWith(ForceXercesParserSoLogbackDoesNotBlowUpWhenUsingSwiftMqClient.class)
@TestPropertySource(properties = {"test.pds.username=e2e-test"})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
//@RequiredArgsConstructor
class RepositoryE2ETest {
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
    private final Gp2gpMessengerProperties gp2GpMessengerProperties;
    private final NhsProperties nhsProperties;

    @Autowired
    public RepositoryE2ETest(TransferTrackerService transferTrackerService, RepoService repoService, TemplatingService templatingService, SimpleAmqpQueue mhsInboundQueue, Gp2GpMessengerOQ gp2gpMessengerOQ, EhrTransferServiceTransferCompleteOQ ehrTransferServiceTransferCompleteOQ, EhrTransferServiceUnhandledOQ ehrTransferServiceUnhandledOQ, EhrTransferServiceLargeEhrFragmentsOQ ehrTransferServiceLargeEhrFragmentsOQ, EhrTransferServiceSmallEhrOQ ehrTransferServiceSmallEhrOQ, EhrTransferServiceLargeEhrOQ ehrTransferServiceLargeEhrOQ, EhrTransferServiceNegativeAcknowledgementOQ ehrTransferServiceNegativeAcknowledgementOQ, EhrTransferServiceParsingDeadLetterQueue ehrTransferServiceParsingDeadLetterQueue, Gp2gpMessengerProperties gp2GpMessengerProperties, NhsProperties nhsProperties) {
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
        this.gp2GpMessengerProperties = gp2GpMessengerProperties;
        this.nhsProperties = nhsProperties;
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

    /**
     * <p>Ensure that a received EHR-out request is identified and forwarded to the ehr-out-service.</p>
     * <ul>
     *     <li>Simulate the receipt of a GP2GP EHR request message via the mhsInboundQueue to be processed at
     *     the ehr-transfer-service.
     *     </li>
     *     <li>As the message ConversationId does not exist in the transfer tracker db, the EHR request
     *     message is forwarded to the ehr-out-service.</li>
     * </ul>
     */
    @Test
    void shouldIdentifyEhrOutRequestWhenConversationIdIsUnrecognised() {
        // Given we have a GPG2GP EHR request message containing an unrecognised ConversationId
        final String outboundConversationId = UUID.randomUUID().toString();
        final String nhsNumber = "9727018440";
        final String sendingOdsCode = "B85002";

        final String ehrRequestMessage = this.templatingService.getTemplatedString(EHR_REQUEST, EhrRequestTemplateContext.builder()
                .messageId(UUID.randomUUID().toString())
                .sendingOdsCode(sendingOdsCode)
                .nhsNumber(nhsNumber)
                .outboundConversationId(outboundConversationId)
                .build());

        // When the EHR request message is added to the mhsInboundQueue
        mhsInboundQueue.sendMessage(ehrRequestMessage, outboundConversationId);

        // Then the ehr-transfer-service will forward the message onto the ehr-out-service
        assertThat(ehrTransferServiceUnhandledOQ.getMessageContaining(ehrRequestMessage)).isNotNull();
    }

    /**
     * <p>Ensure that a small EHR is successfully transferred in and out of the repository.</p>
     * <ul>
     *     <li>Add an entry to the transfer tracker db, bypassing the repo-incoming-queue and sending of an EHR
     *     request by the ehr-transfer-service.</li>
     *     <li>Simulate the receipt of a small EHR from an FSS/GP via the mhsInboundQueue and ensure this is
     *     transferred into the repository.</li>
     *     <li>Simulate the receipt of a EHR out request from an FSS/GP via the mhsInboundQueue.</li>
     *     <li>Assert that the EHR is sent out to the FSS/GP.</li>
     *     <li>Assert that the outgoing EHR payload is identical to that received.</li>
     * </ul>
     */
    // TODO: Assert on the ODS code / other metadata when inspecting the outgoing payload.
    // TODO: conversationId being used in place of correlationId and TraceId. Should remove this for clarity.
    // TODO: Use different ODS codes for receiving and sending practices.
    @Test
    void shouldTransferASmallEhrInAndOut() {
        String inboundConversationId = UUID.randomUUID().toString();
        String outboundConversationId = UUID.randomUUID().toString();
        String nhsNumber = "9727018440";
        String sendingOdsCode = "M85019";
        String asidCode = "200000000149";

        /*
        ORC-IN
         */

        // Given a small EHR is transferred into the repository

        // create entry in transfer tracker db with status ACTION:EHR_REQUEST_SENT
        this.transferTrackerService.save(ConversationRecord.builder()
                .inboundConversationId(inboundConversationId)
                .nemsMessageId(randomNemsMessageId())
                .nhsNumber(nhsNumber)
                .sourceGp(sendingOdsCode)
                .transferStatus(INBOUND_REQUEST_SENT.name())
                .build());

        // Construct small EHR message
        SmallEhrTemplateContext smallEhrTemplateContext = SmallEhrTemplateContext.builder()
                .inboundConversationId(inboundConversationId.toUpperCase())
                .nhsNumber(nhsNumber)
                .build();
        String smallEhrMessage = this.templatingService.getTemplatedString(TemplateVariant.SMALL_EHR_WITHOUT_LINEBREAKS, smallEhrTemplateContext);

        // Put the patient EHR onto the mhsInboundQueue
        mhsInboundQueue.sendMessage(smallEhrMessage, inboundConversationId);

        // Wait until the patient EHR is successfully transferred to the repository
        log.info("inbound conversationId: {}", inboundConversationId);
        log.info("outbound conversationId: {}", outboundConversationId);
        log.info("conversationIdExists: {}", transferTrackerService.conversationIdExists(inboundConversationId));
        String status = transferTrackerService.waitForStatusMatching(inboundConversationId, INBOUND_COMPLETE.name());
        log.info("tracker db status: {}", status);

        /*
        ORC-OUT
         */

        // When an EHR OUT request is received

        // Construct an EHR request
        EhrRequestTemplateContext ehrRequestTemplateContext = EhrRequestTemplateContext.builder()
                .outboundConversationId(outboundConversationId.toUpperCase())
                .nhsNumber(nhsNumber)
                .sendingOdsCode(sendingOdsCode)
                .asidCode(asidCode)
                .build();
        String ehrRequestMessage = this.templatingService.getTemplatedString(EHR_REQUEST, ehrRequestTemplateContext);

        // Add EHR request to mhsInboundQueue
        mhsInboundQueue.sendMessage(ehrRequestMessage, outboundConversationId);

        // Then the patient EHR is transferred to the requesting practice

        // Assert that the outgoing EHR is added to the gp2gp messenger observability queue
        SqsMessage gp2gpMessage = gp2gpMessengerOQ.getMessageContaining(outboundConversationId);
        assertThat(gp2gpMessage).isNotNull();
        assertTrue(gp2gpMessage.contains(EHR_CORE.interactionId));

        // Compare the received EHR payload with the outgoing EHR payload and assert they are identical
        String gp2gpMessengerPayload = getPayloadOptional(gp2gpMessage.getBody()).orElseThrow();
        String smallEhrPayload = getPayloadOptional(smallEhrMessage).orElseThrow();

        log.info("Payload from gp2gpMessenger: {}", gp2gpMessengerPayload);
        log.info("Payload from smallEhr: {}", smallEhrPayload);

        Diff myDiff = comparePayloads(gp2gpMessengerPayload, smallEhrPayload);
        assertFalse(myDiff.toString(), myDiff.hasDifferences());
    }

    /**
     * <p>Ensure that a large EHR is successfully transferred in and out of the repository.</p>
     * <ul>
     *     <li>Add an entry to the transfer tracker db, bypassing the repo-incoming-queue and sending of an EHR
     *     request by the ehr-transfer-service.</li>
     *     <li>Simulate the receipt of a large EHR core + 2 fragments from an FSS/GP via the mhsInboundQueue
     *     and ensure this is transferred into the repository.</li>
     *     <li>Simulate the receipt of a EHR out request from an FSS/GP via the mhsInboundQueue.</li>
     *     <li>Assert that the EHR is sent out to the FSS/GP.</li>
     *     <li>Assert that the outgoing EHR payload is identical to that received.</li>
     * </ul>
     */
    // TODO: Assert on the ODS code / other metadata when inspecting the outgoing payload.
    // TODO: conversationId being used in place of correlationId and TraceId. Should remove this for clarity.
    // TODO: Use different ODS codes for receiving and sending practices.
    @Test
    void shouldTransferALargeEHRInAndOut() {
        String inboundConversationId = UUID.randomUUID().toString();
        String outboundConversationId = UUID.randomUUID().toString();
        String largeEhrCoreMessageId = UUID.randomUUID().toString();
        String fragment1MessageId = UUID.randomUUID().toString();
        String fragment2MessageId = UUID.randomUUID().toString();
        String nhsNumber = "9727018440";
        String sendingOdsCode = "M85019";
        String recipientOdsCode = "M85019";
        String asidCode = "200000000149";

        /*
        ORC-IN
         */

        // Given a large EHR is transferred into the repository

        // create entry in transfer tracker db with status ACTION:EHR_REQUEST_SENT



        this.transferTrackerService.save(ConversationRecord.builder()
                .inboundConversationId(inboundConversationId)
                .nhsNumber(nhsNumber)
                .transferStatus(INBOUND_REQUEST_SENT.name())
                .nemsMessageId(randomNemsMessageId())
                .sourceGp(sendingOdsCode)
                .build()
        );

        // Construct large EHR core message
        LargeEhrCoreTemplateContext largeEhrCoreTemplateContext = LargeEhrCoreTemplateContext.builder()
                .inboundConversationId(inboundConversationId.toUpperCase())
                .largeEhrCoreMessageId(largeEhrCoreMessageId.toUpperCase())
                .fragmentMessageId(fragment1MessageId)
                .nhsNumber(nhsNumber)
                .senderOdsCode(sendingOdsCode)
                .build();
        String largeEhrCore = this.templatingService.getTemplatedString(TemplateVariant.LARGE_EHR_CORE, largeEhrCoreTemplateContext);

        // Put the large EHR core message onto the mhsInboundQueue
        mhsInboundQueue.sendMessage(largeEhrCore, inboundConversationId);
        log.info("added EHR IN message containing large EHR core to mhsInboundQueue");
        log.info("conversationId {} exists in transferTrackerDb: {}", inboundConversationId, transferTrackerService.conversationIdExists(inboundConversationId));

        // Wait until the large EHR core is processed by the ehr-transfer-service
        String status = transferTrackerService.waitForStatusMatching(inboundConversationId, INBOUND_CONTINUE_REQUEST_SENT.name());
        log.info("tracker db status: {}", status);

        // Construct large EHR fragment messages
        List<String> largeEhrFragments = this.templatingService.getMultipleTemplatedStrings(Map.of(
                TemplateVariant.LARGE_EHR_FRAGMENT_WITH_REF, LargeEhrFragmentWithReferencesContext.builder()
                        .inboundConversationId(inboundConversationId.toUpperCase())
                        .fragmentMessageId(fragment1MessageId.toUpperCase())
                        .fragmentTwoMessageId(fragment2MessageId.toUpperCase())
                        .recipientOdsCode(recipientOdsCode)
                        .senderOdsCode(nhsProperties.getRepoOdsCode())
                        .build(),

                TemplateVariant.LARGE_EHR_FRAGMENT_NO_REF, LargeEhrFragmentNoReferencesContext.builder()
                        .inboundConversationId(inboundConversationId.toUpperCase())
                        .fragmentMessageId(fragment2MessageId.toUpperCase())
                        .recipientOdsCode(recipientOdsCode)
                        .senderOdsCode(nhsProperties.getRepoOdsCode())
                        .build()
        ));

        // Put the large fragment messages onto the mhsInboundQueue
        largeEhrFragments.forEach(fragment -> mhsInboundQueue.sendMessage(fragment, inboundConversationId));
        log.info("added EHR IN messages containing large EHR fragments to mhsInboundQueue");
        log.info("fragment 1 message id: {}", fragment1MessageId);
        log.info("fragment 2 message id: {}", fragment2MessageId);

        // Wait until the patient EHR is successfully transferred to the repository
        log.info("Waiting for transferTrackerDb status of INBOUND_COMPLETE");
        status = transferTrackerService.waitForStatusMatching(inboundConversationId, INBOUND_COMPLETE.name());
        log.info("tracker db status: {}", status);

        /*
        ORC-OUT
         */

        // When an EHR out request is received

        // Construct an EHR out request
        EhrRequestTemplateContext ehrRequestTemplateContext = EhrRequestTemplateContext.builder()
                .outboundConversationId(outboundConversationId)
                .nhsNumber(nhsNumber)
                .sendingOdsCode(sendingOdsCode)
                .asidCode(asidCode)
                .build();
        String ehrRequestMessage = this.templatingService.getTemplatedString(EHR_REQUEST, ehrRequestTemplateContext);

        // Add EHR out request to mhsInboundQueue
        mhsInboundQueue.sendMessage(ehrRequestMessage, outboundConversationId);
        log.info("added EHR OUT request to mhsInboundQueue with outboundConversationId {}", outboundConversationId);

        // Then the patient EHR is transferred to the requesting practice

        // Assert that the outgoing EHR core is added to the gp2gp messenger observability queue
        SqsMessage gp2gpMessageUK06 = gp2gpMessengerOQ.getMessageContaining(outboundConversationId);

        assertThat(gp2gpMessageUK06).isNotNull();
        assertTrue(gp2gpMessageUK06.contains(EHR_CORE.interactionId));

        String gp2gpMessengerEhrCorePayload = getPayloadOptional(gp2gpMessageUK06.getBody()).orElseThrow();
        String largeEhrCorePayload = getPayloadOptional(largeEhrCore).orElseThrow();

        Diff compareEhrCores = comparePayloads(gp2gpMessengerEhrCorePayload, largeEhrCorePayload);
        boolean ehrCoreIsIdentical = !compareEhrCores.hasDifferences();
        assertTrue(ehrCoreIsIdentical);

        // construct continue request
        String continueRequestMessage = this.templatingService.getTemplatedString(
                CONTINUE_REQUEST,
                ContinueRequestTemplateContext.builder()
                        .outboundConversationId(outboundConversationId)
                        .senderOdsCode(sendingOdsCode)
                        .recipientOdsCode(recipientOdsCode)
                        .build());

        // Put a continue request to inboundQueueFromMhs
        mhsInboundQueue.sendMessage(
                continueRequestMessage,
                outboundConversationId
        );

        // Get all message fragments from gp2gp-messenger observability queue and compare with inbound fragments
        Set<SqsMessage> allFragments = gp2gpMessengerOQ.getAllMessagesContaining(EHR_FRAGMENT.interactionId, 2);
        assertThat(allFragments.size()).isGreaterThanOrEqualTo(2);

        String largeEhrFragment1Payload = getPayloadOptional(largeEhrFragments.get(0)).orElseThrow();
        String largeEhrFragment2Payload = getPayloadOptional(largeEhrFragments.get(1)).orElseThrow();

        allFragments.forEach(fragment -> {
            assertTrue(fragment.contains(outboundConversationId));

            String fragmentPayload = getPayloadOptional(fragment.getBody()).orElseThrow();
            Diff compareWithFragment1 = comparePayloads(fragmentPayload, largeEhrFragment1Payload);
            Diff compareWithFragment2 = comparePayloads(fragmentPayload, largeEhrFragment2Payload);

            boolean identicalWithFragment1 = !compareWithFragment1.hasDifferences();
            boolean identicalWithFragment2 = !compareWithFragment2.hasDifferences();

            templatingService.getTemplatedString(SMALL_EHR, SmallEhrTemplateContext.builder().build());

            assertTrue(identicalWithFragment1 || identicalWithFragment2);
        });
    }

    // TODO: ABSTRACT THIS OUT TO ANOTHER CLASS
    private Arguments erroneousInboundMessage_UnrecognisedInteractionID() {
        String invalidInteractionId = "TEST_XX123456XX01";
        String nhsNumber = Patient.PATIENT_WITH_SMALL_EHR_IN_REPO_AND_MOF_SET_TO_TPP.nhsNumber();
        String sendingOdsCode = Gp2GpSystem.TPP_PTL_INT.odsCode();

        EhrRequestTemplateContext ehrRequestContext = EhrRequestTemplateContext.builder()
                .nhsNumber(nhsNumber)
                .sendingOdsCode(sendingOdsCode)
                .build();
        String inboundMessage = this.templatingService.getTemplatedString(EHR_REQUEST, ehrRequestContext);

        String erroneousInboundMessage = inboundMessage
                .replaceAll(MessageType.EHR_REQUEST.interactionId, invalidInteractionId);

        return Arguments.of(
                Named.of("Message with unrecognised Interaction ID", erroneousInboundMessage),
                ehrRequestContext.getOutboundConversationId()
        );
    }

    // TODO: ABSTRACT THIS OUT TO ANOTHER CLASS
    private Arguments erroneousInboundMessage_EhrRequestWithUnrecognisedNhsNumber() {
        String nonExistentNhsNumber = "9729999999";
        String newGpOdsCode = Gp2GpSystem.TPP_PTL_INT.odsCode();

        EhrRequestTemplateContext ehrRequestContext = EhrRequestTemplateContext.builder()
                .nhsNumber(nonExistentNhsNumber)
                .sendingOdsCode(newGpOdsCode)
                .build();
        String erroneousInboundMessage = this.templatingService.getTemplatedString(EHR_REQUEST, ehrRequestContext);

        return Arguments.of(
                Named.of("EHR Request with unrecognised NHS Number", erroneousInboundMessage),
                ehrRequestContext.getOutboundConversationId()
        );
    }

    // TODO: ABSTRACT THIS OUT TO ANOTHER CLASS
    private Arguments erroneousInboundMessage_ContinueRequestWithUnrecognisedConversationId() {
        ContinueRequestTemplateContext continueRequestContext = ContinueRequestTemplateContext.builder()
                .recipientOdsCode(TPP_PTL_INT.odsCode())
                .senderOdsCode(EMIS_PTL_INT.odsCode())
                .build();

        String continueRequestMessage = this.templatingService
                .getTemplatedString(CONTINUE_REQUEST, continueRequestContext);

        return Arguments.of(
                Named.of("Continue Request with unrecognised Conversation ID", continueRequestMessage),
                continueRequestContext.getOutboundConversationId()
        );
    }

    // TODO: ABSTRACT THIS OUT TO ANOTHER CLASS
    private Stream<Arguments> erroneousInboundMessages() {
        return Stream.of(
                erroneousInboundMessage_UnrecognisedInteractionID(),
                erroneousInboundMessage_EhrRequestWithUnrecognisedNhsNumber(),
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
    @ParameterizedTest(name = "[Should reject {0}")
    @MethodSource("erroneousInboundMessages")
    @DisplayName("Should reject erroneous inbound EHR request messages")
    void shouldRejectErroneousEhrRequestMessages(String inboundMessage, String conversationId) {
        // Given that we have an erroneous inbound EHR request message
        // When the message is received via the mhsInboundQueue
        mhsInboundQueue.sendMessage(inboundMessage, conversationId.toLowerCase());

        // Then the ehr-transfer-service will reject the message via the ehr-transfer-service-unhandled-queue
        SqsMessage unhandledMessage = ehrTransferServiceUnhandledOQ.getMessageContaining(conversationId);
        assertThat(unhandledMessage.getBody()).isEqualTo(inboundMessage);

        assertTrue(gp2gpMessengerOQ.verifyNoMessageContaining(conversationId));
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
        String nhsNumber = Patient.PATIENT_WITH_SMALL_EHR_IN_REPO_AND_MOF_SET_TO_TPP.nhsNumber();

        // Given a small EHR exists in the repository
        repoService.addSmallEhrToEhrRepo(nhsNumber, SMALL_EHR);

        // When 2 identical EHR out requests are received (from same GP, using same ConversationId)

        // Construct an EHR request message
        EhrRequestTemplateContext templateContext = EhrRequestTemplateContext.builder()
                .nhsNumber(nhsNumber)
                .sendingOdsCode(TPP_PTL_INT.odsCode())
                .asidCode(TPP_PTL_INT.asidCode()).build();
        String ehrRequestMessage = this.templatingService.getTemplatedString(EHR_REQUEST, templateContext);
        String conversationId = templateContext.getOutboundConversationId();

        // Send 2 EHR out requests
        for (int i = 0; i < 2; i++) {
            mhsInboundQueue.sendMessage(ehrRequestMessage, conversationId);
            log.info("Duplicate EHR Request {} of {} sent to MHS Inbound queue successfully.", (i + 1), 2);
        }
        log.info("EHR out request ConversationId: {}", conversationId);

        // Then only one EHR is transferred to the requesting practice
        boolean messagesFound = this.gp2gpMessengerOQ.getAllMessagesFromQueueWithConversationIds(1, 0,
                List.of(conversationId));
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
        String nhsNumber = Patient.PATIENT_WITH_SMALL_EHR_IN_REPO_AND_MOF_SET_TO_TPP.nhsNumber();

        // Given a small EHR exists in the repository
        repoService.addSmallEhrToEhrRepo(nhsNumber, SMALL_EHR);

        // When 2 identical EHR out requests are received (from same GP, using same ConversationId)

        // Construct an EHR request message
        EhrRequestTemplateContext templateContext1 = EhrRequestTemplateContext.builder()
                .nhsNumber(nhsNumber)
                .sendingOdsCode(TPP_PTL_INT.odsCode())
                .asidCode(TPP_PTL_INT.asidCode()).build();
        String ehrRequestMessage1 = this.templatingService.getTemplatedString(EHR_REQUEST, templateContext1);
        String conversationId1 = templateContext1.getOutboundConversationId();

        // Construct an EHR request message
        EhrRequestTemplateContext templateContext2 = EhrRequestTemplateContext.builder()
                .nhsNumber(nhsNumber)
                .sendingOdsCode(TPP_PTL_INT.odsCode())
                .asidCode(TPP_PTL_INT.asidCode()).build();
        String ehrRequestMessage2 = this.templatingService.getTemplatedString(EHR_REQUEST, templateContext2);
        String conversationId2 = templateContext2.getOutboundConversationId();

        // Send 2 EHR out requests
        mhsInboundQueue.sendMessage(ehrRequestMessage1, conversationId1);
        mhsInboundQueue.sendMessage(ehrRequestMessage2, conversationId2);
        log.info(
                "added EHR out requests to mhsInboundQueue. ConversationId1: {}, ConversationId2: {}",
                conversationId1,
                conversationId2
        );

        // Then only one EHR is transferred to the requesting practice
        boolean messagesFound = this.gp2gpMessengerOQ.getAllMessagesFromQueueWithConversationIds(1, 0,
                List.of(conversationId1, conversationId2));
        assertTrue(messagesFound);
    }

    /**
     * Ensure that a small EHR with 99 attachments is transferred in and out of the repository successfully.
     * <ul>
     *     <li>Add an entry to the transfer tracker db, bypassing the repo-incoming-queue and sending of an EHR
     *     request by the ehr-transfer-service.</li>
     *     <li>Simulate the receipt of a small EHR from an FSS/GP via the mhsInboundQueue and ensure this is
     *     transferred into the repository.</li>
     *     <li>Simulate the receipt of a EHR out request from an FSS/GP via the mhsInboundQueue.</li>
     *     <li>Assert that the EHR is sent out to the FSS/GP.</li>
     */
    @Test
    void shouldTransferASmallEhrWith99AttachmentsInAndOut() {
        // Given a small EHR with 99 attachments exists in the repository
        String nhsNumber = Patient.PATIENT_WITH_SMALL_EHR_IN_REPO_AND_MOF_SET_TO_TPP.nhsNumber();
        this.repoService.addSmallEhrToEhrRepo(nhsNumber, SMALL_EHR_WITH_99_ATTACHMENTS);

        // When an EHR out request is received
        EhrRequestTemplateContext templateContext = EhrRequestTemplateContext.builder()
                .nhsNumber(nhsNumber)
                .sendingOdsCode(TPP_PTL_INT.odsCode())
                .asidCode(TPP_PTL_INT.asidCode()).build();
        String ehrRequestMessage = this.templatingService.getTemplatedString(EHR_REQUEST, templateContext);
        String conversationId = templateContext.getOutboundConversationId();

        mhsInboundQueue.sendMessage(ehrRequestMessage, conversationId);

        // Then the patient EHR is transferred to the requesting practice
        boolean messageFound = this.gp2gpMessengerOQ.getAllMessagesFromQueueWithConversationIds(1, 0,
                List.of(conversationId));

        // then
        assertTrue(messageFound);
    }

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
        String nhsNumber = Patient.PATIENT_WITH_SMALL_EHR_IN_REPO_AND_MOF_SET_TO_TPP.nhsNumber();
        String gpOdsCode = EMIS_PTL_INT.odsCode();

        // Given a small EHR exists in the repository
        this.repoService.addSmallEhrToEhrRepo(nhsNumber, SMALL_EHR);

        // When an EHR out request is received from a GP where the patient is not registered (different ODS code)
        EhrRequestTemplateContext templateContext = EhrRequestTemplateContext.builder()
                .nhsNumber(nhsNumber)
                .sendingOdsCode(gpOdsCode)
                .asidCode(TPP_PTL_INT.asidCode())
                .build();
        String ehrRequestMessage = this.templatingService.getTemplatedString(EHR_REQUEST, templateContext);
        String conversationId = templateContext.getOutboundConversationId();

        mhsInboundQueue.sendMessage(ehrRequestMessage, conversationId);
        log.info("EHR out request sent successfully. ConversationId: {}", conversationId);

        // Then the EHR request is rejected

        // Assert that the EHR has not been sent
        assertTrue(gp2gpMessengerOQ.verifyNoMessageContaining(conversationId, 20));

        // TODO: assert that ehr-transfer-tracker.FailureReason = 'OUTBOUND:incorrect_ods_code' (awaiting db refactor)

    }

//    @Test
//    void shouldNotIngestDuplicateEhr() {
//        String nhsNumber = Patient.PATIENT_WITH_SMALL_EHR_IN_REPO_AND_MOF_SET_TO_TPP.nhsNumber();
//
//        // Given a small EHR exists in the repository
//        this.repoService.addSmallEhrToEhrRepo(nhsNumber, SMALL_EHR);
//
//        this.repoService.addSmallEhrToEhrRepo(nhsNumber, SMALL_EHR);
//    }

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
        String NEGATIVE_ACKNOWLEDGEMENT_FAILURE_CODE = "30";
        UUID ackMessageId = UUID.randomUUID();
        String inboundConversationId = UUID.randomUUID().toString();
        String odsCode = gp2GpMessengerProperties.getTppPtlIntOdsCode();

        // Given that an EHR request has been sent from the repository to an FSS

        // create entry in transfer tracker db with status ACTION:EHR_REQUEST_SENT
        this.transferTrackerService.save(ConversationRecord.builder()
                .inboundConversationId(inboundConversationId)
                .nemsMessageId(randomNemsMessageId())
                .nhsNumber(Patient.SUSPENDED_WITH_EHR_AT_TPP.nhsNumber())
                .sourceGp(odsCode)
                .transferStatus(INBOUND_REQUEST_SENT.name())
                .build()
        );

        // When a negative acknowledgement message is received
        String ackMessage = this.templatingService.getTemplatedString(NEGATIVE_ACKNOWLEDGEMENT,
                AcknowledgementTemplateContext.builder()
                        .messageId(ackMessageId)
                        .inboundConversationId(inboundConversationId)
                        .build());
        mhsInboundQueue.sendMessage(ackMessage, inboundConversationId);
        log.info("negative acknowledgement message successfully added for stubbed EHR request with conversationId: {}", inboundConversationId);

        // Then the negative acknowledgement message is processed and the transferTrackerStatus is updated as expected
        assertThat(ehrTransferServiceNegativeAcknowledgementOQ.getMessageContaining(inboundConversationId)).isNotNull();
        assertThat(ehrTransferServiceTransferCompleteOQ.getMessageContainingAttribute("conversationId", inboundConversationId)).isNotNull();

        String status = transferTrackerService.waitForStatusMatching(inboundConversationId, OldTransferTrackerStatus.EHR_TRANSFER_FAILED.status);
        assertThat(status).isEqualTo(OldTransferTrackerStatus.EHR_TRANSFER_FAILED.status + ":" + NEGATIVE_ACKNOWLEDGEMENT_FAILURE_CODE);
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
                UUID.randomUUID().toString()
        );

        unexpectedMessages.forEach(message -> {
            mhsInboundQueue.sendUnexpectedMessage(message);
            assertThat(ehrTransferServiceParsingDeadLetterQueue.getMessageContaining(message)).isNotNull();
        });
    }

    @Test
    void shouldPutASmallEHROntoRepoAndSendEHRToMHSOutboundWhenReceivingRequestFromGP() {
        // Given
        String inboundConversationId = UUID.randomUUID().toString();
        String outboundConversationId = UUID.randomUUID().toString();
        String nhsNumber = "9727018440";
        String sendingOdsCode = TPP_PTL_INT.odsCode();
        String asidCode = TPP_PTL_INT.asidCode();

        log.info(" ===============  outboundConversationId: {}", outboundConversationId);

        String smallEhr = templatingService.getTemplatedString(SMALL_EHR, SmallEhrTemplateContext.builder()
                .inboundConversationId(inboundConversationId.toUpperCase())
                .nhsNumber(nhsNumber)
                .build());

        String ehrRequest = templatingService.getTemplatedString(EHR_REQUEST, EhrRequestTemplateContext.builder()
                .nhsNumber(nhsNumber)
                .outboundConversationId(outboundConversationId)
                .sendingOdsCode(sendingOdsCode)
                .asidCode(asidCode)
                .build());

        // When
        // change transfer db status to ACTION:EHR_REQUEST_SENT before putting on inbound queue
        transferTrackerService.save(ConversationRecord.builder()
                        .inboundConversationId(inboundConversationId)
                        .nemsMessageId(randomNemsMessageId())
                        .nhsNumber(nhsNumber)
                        .sourceGp(sendingOdsCode)
                        .transferStatus(INBOUND_REQUEST_SENT.name())
                        .build());

        // Put the patient into mhsInboundQueue as a UK05 message
        mhsInboundQueue.sendMessage(smallEhr, inboundConversationId);

        log.info("conversationId: {}, Exists: {}", inboundConversationId, transferTrackerService.conversationIdExists(inboundConversationId));

        String status = transferTrackerService.waitForStatusMatching(
                inboundConversationId,
                INBOUND_COMPLETE.name());

        log.info("tracker db status: {}", status);

        // Send an EHR request from mhsInboundQueue
        mhsInboundQueue.sendMessage(ehrRequest, outboundConversationId);

        // Then
        SqsMessage gp2gpMessage = gp2gpMessengerOQ.getMessageContaining(outboundConversationId);

        String gp2gpMessengerPayload = getPayloadOptional(gp2gpMessage.getBody()).orElseThrow();
        String smallEhrPayload = getPayloadOptional(smallEhr).orElseThrow();
        log.info("Payload from gp2gpMessenger: {}", gp2gpMessengerPayload);
        log.info("Payload from smallEhr: {}", smallEhrPayload);

        assertThat(gp2gpMessage).isNotNull();
        assertTrue(gp2gpMessage.contains(EHR_CORE.interactionId));
        assertTrue(gp2gpMessengerPayload.contains(nhsNumber));

        // clear up the queue after test in order not to interfere with other tests
        gp2gpMessengerOQ.deleteMessage(gp2gpMessage);
    }

    @Test
    void shouldPutALargeEHROntoRepoAndSendEHRToMHSOutboundWhenReceivingRequestFromGP() {
        // given
        String inboundConversationId = UUID.randomUUID().toString();
        String outboundConversationId = UUID.randomUUID().toString();
        log.info(" ===============  inboundConversationId: {}", inboundConversationId);
        log.info(" ===============  outboundConversationId: {}", outboundConversationId);

        String largeEhrCoreMessageId = UUID.randomUUID().toString();
        String fragment1MessageId = UUID.randomUUID().toString();
        String fragment2MessageId = UUID.randomUUID().toString();
        String nhsNumber = "9727018157";
        String senderOdsCode = TPP_PTL_INT.odsCode();
        String recipientOdsCode = EMIS_PTL_INT.odsCode();
        String asidCode = TPP_PTL_INT.asidCode();

        String largeEhrCore = this.templatingService.getTemplatedString(LARGE_EHR_CORE,
                LargeEhrCoreTemplateContext.builder()
                        .inboundConversationId(inboundConversationId.toUpperCase())
                        .largeEhrCoreMessageId(largeEhrCoreMessageId.toUpperCase())
                        .fragmentMessageId(fragment1MessageId.toUpperCase())
                        .nhsNumber(nhsNumber)
                        .senderOdsCode(senderOdsCode)
                        .build());

        String largeEhrFragment1 = this.templatingService.getTemplatedString(LARGE_EHR_FRAGMENT_WITH_REF,
                LargeEhrFragmentWithReferencesContext.builder()
                        .inboundConversationId(inboundConversationId.toUpperCase())
                        .fragmentMessageId(fragment1MessageId.toUpperCase())
                        .fragmentTwoMessageId(fragment2MessageId.toUpperCase())
                        .recipientOdsCode(recipientOdsCode)
                        .senderOdsCode(nhsProperties.getRepoOdsCode())
                        .build());

        String largeEhrFragment2 = this.templatingService.getTemplatedString(LARGE_EHR_FRAGMENT_NO_REF,
                LargeEhrFragmentNoReferencesContext.builder()
                        .inboundConversationId(inboundConversationId.toUpperCase())
                        .fragmentMessageId(fragment2MessageId.toUpperCase())
                        .recipientOdsCode(recipientOdsCode)
                        .senderOdsCode(nhsProperties.getRepoOdsCode())
                        .build());

        String ehrRequest = this.templatingService.getTemplatedString(EHR_REQUEST,
                EhrRequestTemplateContext.builder()
                        .outboundConversationId(outboundConversationId.toUpperCase())
                        .nhsNumber(nhsNumber)
                        .sendingOdsCode(senderOdsCode)
                        .asidCode(asidCode)
                        .build());

        String continueRequest = this.templatingService.getTemplatedString(CONTINUE_REQUEST,
                ContinueRequestTemplateContext.builder()
                        .outboundConversationId(outboundConversationId.toUpperCase())
                        .recipientOdsCode(recipientOdsCode)
                        .senderOdsCode(senderOdsCode)
                        .build());

        transferTrackerService.save(ConversationRecord.builder()
                .inboundConversationId(inboundConversationId)
                .nemsMessageId(randomNemsMessageId())
                .nhsNumber(nhsNumber)
                .sourceGp(senderOdsCode)
                .transferStatus(INBOUND_REQUEST_SENT.name())
                .build());
        
        // when
        mhsInboundQueue.sendMessage(largeEhrCore, inboundConversationId);

        log.info("conversationIdExists: {}", transferTrackerService.conversationIdExists(inboundConversationId));
        String status = transferTrackerService.waitForStatusMatching(inboundConversationId, INBOUND_REQUEST_SENT.name());
        log.info("tracker db status: {}", status);
        log.info("fragment 1 message id: {}", fragment1MessageId);
        log.info("fragment 2 message id: {}", fragment2MessageId);

        mhsInboundQueue.sendMessage(largeEhrFragment1, inboundConversationId);
        mhsInboundQueue.sendMessage(largeEhrFragment2, inboundConversationId);

        status = transferTrackerService.waitForStatusMatching(inboundConversationId, INBOUND_COMPLETE.name());
            log.info("tracker db status: {}", status);

        // Put a EHR request to inboundQueueFromMhs
        mhsInboundQueue.sendMessage(ehrRequest, outboundConversationId);

        // Then
        // assert gp2gpMessenger queue got a message of UK06
        SqsMessage gp2gpMessageUK06 = gp2gpMessengerOQ.getMessageContaining(outboundConversationId);

        assertThat(gp2gpMessageUK06).isNotNull();
        assertThat(gp2gpMessageUK06.contains(EHR_CORE.interactionId)).isTrue();

        // Put a continue request to inboundQueueFromMhs
        mhsInboundQueue.sendMessage(continueRequest, outboundConversationId);

        // get all message fragments from gp2gp-messenger observability queue and compare with inbound fragments
        Set<SqsMessage> allFragments = gp2gpMessengerOQ.getAllMessagesContaining(EHR_FRAGMENT.interactionId, 2);

        assertThat(allFragments.size()).isEqualTo(2);

        allFragments.forEach(fragment -> assertThat(fragment.contains(outboundConversationId)).isTrue());

        // clear up the queue after test in order not to interfere with other tests
        gp2gpMessengerOQ.deleteMessage(gp2gpMessageUK06);
        allFragments.forEach(gp2gpMessengerOQ::deleteMessage);
    }
}
