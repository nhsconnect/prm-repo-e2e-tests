package uk.nhs.prm.e2etests.test.repositorye2etest;

import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.xmlunit.diff.Diff;

import uk.nhs.prm.e2etests.enumeration.*;
import uk.nhs.prm.e2etests.model.SqsMessage;
import uk.nhs.prm.e2etests.model.database.ConversationRecord;
import uk.nhs.prm.e2etests.model.templatecontext.ContinueRequestTemplateContext;
import uk.nhs.prm.e2etests.model.templatecontext.EhrRequestTemplateContext;
import uk.nhs.prm.e2etests.model.templatecontext.LargeEhrCoreTemplateContext;
import uk.nhs.prm.e2etests.model.templatecontext.LargeEhrFragmentNoReferencesContext;
import uk.nhs.prm.e2etests.model.templatecontext.LargeEhrFragmentWithReferencesContext;
import uk.nhs.prm.e2etests.model.templatecontext.SmallEhrTemplateContext;
import uk.nhs.prm.e2etests.property.NhsProperties;
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
import uk.nhs.prm.e2etests.service.TransferTrackerService;
import uk.nhs.prm.e2etests.test.ForceXercesParserSoLogbackDoesNotBlowUpWhenUsingSwiftMqClient;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.util.AssertionErrors.assertFalse;

import static uk.nhs.prm.e2etests.enumeration.ConversationTransferStatus.*;
import static uk.nhs.prm.e2etests.enumeration.Gp2GpSystem.EMIS_PTL_INT;
import static uk.nhs.prm.e2etests.enumeration.Gp2GpSystem.TPP_PTL_INT;
import static uk.nhs.prm.e2etests.enumeration.MessageType.EHR_CORE;
import static uk.nhs.prm.e2etests.enumeration.MessageType.EHR_FRAGMENT;
import static uk.nhs.prm.e2etests.enumeration.Patient.PATIENT_WITH_SMALL_EHR_IN_REPO_AND_MOF_SET_TO_TPP;
import static uk.nhs.prm.e2etests.enumeration.TemplateVariant.CONTINUE_REQUEST;
import static uk.nhs.prm.e2etests.enumeration.TemplateVariant.EHR_REQUEST;
import static uk.nhs.prm.e2etests.enumeration.TemplateVariant.LARGE_EHR_CORE;
import static uk.nhs.prm.e2etests.enumeration.TemplateVariant.LARGE_EHR_FRAGMENT_NO_REF;
import static uk.nhs.prm.e2etests.enumeration.TemplateVariant.LARGE_EHR_FRAGMENT_WITH_REF;
import static uk.nhs.prm.e2etests.enumeration.TemplateVariant.SMALL_EHR;
import static uk.nhs.prm.e2etests.enumeration.TemplateVariant.SMALL_EHR_WITH_99_ATTACHMENTS;
import static uk.nhs.prm.e2etests.property.TestConstants.*;
import static uk.nhs.prm.e2etests.utility.XmlComparisonUtility.comparePayloads;
import static uk.nhs.prm.e2etests.utility.XmlComparisonUtility.getPayloadOptional;

@Log4j2
@SpringBootTest
@ExtendWith(ForceXercesParserSoLogbackDoesNotBlowUpWhenUsingSwiftMqClient.class)
@TestPropertySource(properties = {"test.pds.username=e2e-test"})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RepositoryE2ETest {
    private final TransferTrackerService transferTrackerService;
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
    public RepositoryE2ETest(
            TransferTrackerService transferTrackerService,
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
        this.transferTrackerService = transferTrackerService;
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

    @AfterEach
    void afterEach() throws InterruptedException {
        transferTrackerService.clearConversation(inboundConversationId);
        Thread.sleep(1000);
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
        final String nhsNumber = PATIENT_WITH_SMALL_EHR_IN_REPO_AND_MOF_SET_TO_TPP.nhsNumber();
        log.info("nhsNumber: " + nhsNumber);

        final String ehrRequestMessage = this.templatingService.getTemplatedString(EHR_REQUEST, EhrRequestTemplateContext.builder()
                .messageId(messageId)
                .senderOdsCode(senderOdsCode)
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
        final String nhsNumber = PATIENT_WITH_SMALL_EHR_IN_REPO_AND_MOF_SET_TO_TPP.nhsNumber();
        final String senderOdsCode = TPP_PTL_INT.odsCode();
        final String asidCode = TPP_PTL_INT.asidCode();

        log.info("nhsNumber: " + nhsNumber);
        log.info("senderOdsCode: " + senderOdsCode);
        log.info("asidCode: " + asidCode);

        /*
        ORC-IN
         */

        // Given a small EHR is transferred into the repository
        // create entry in transfer tracker db with status INBOUND_REQUEST_SENT
        this.transferTrackerService.saveConversation(ConversationRecord.builder()
                .inboundConversationId(inboundConversationId)
                .nemsMessageId(nemsMessageId)
                .nhsNumber(nhsNumber)
                .sourceGp(senderOdsCode)
                .transferStatus(INBOUND_REQUEST_SENT.name())
                .associatedTest(testName)
                .build());

        // Construct small EHR message
        SmallEhrTemplateContext smallEhrTemplateContext = SmallEhrTemplateContext.builder()
                .inboundConversationId(inboundConversationId)
                .nhsNumber(nhsNumber)
                .build();

        String smallEhrMessage = this.templatingService.getTemplatedString(TemplateVariant.SMALL_EHR_WITHOUT_LINEBREAKS, smallEhrTemplateContext);

        // Put the patient EHR onto the mhsInboundQueue
        mhsInboundQueue.sendMessage(smallEhrMessage, inboundConversationId);

        // Wait until the patient EHR is successfully transferred to the repository
        log.info("conversationIdExists: {}", transferTrackerService.inboundConversationIdExists(inboundConversationId));
        String status = transferTrackerService.waitForConversationTransferStatusMatching(inboundConversationId, INBOUND_COMPLETE.name());
        log.info("tracker db status: {}", status);

        /*
        ORC-OUT
         */

        // When an EHR OUT request is received

        // Construct an EHR request
        EhrRequestTemplateContext ehrRequestTemplateContext = EhrRequestTemplateContext.builder()
                .outboundConversationId(outboundConversationId)
                .nhsNumber(nhsNumber)
                .senderOdsCode(senderOdsCode)
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
     *
     This test scenario involves the transfer of a large Electronic Health Record (EHR) in and out of a system. Here's a plain English summary:

     Inbound Transfer (ORC-IN):

     A large EHR is sent into the system.
     An entry is created in the transfer tracker database indicating the inbound request has been sent.
     The large EHR core message is constructed and sent to the system.
     The system processes the large EHR core message.
     Large EHR fragment messages are constructed and sent to the system.
     The system processes the large EHR fragment messages.
     The transfer tracker database is updated to indicate the inbound transfer is complete.

     Outbound Transfer (ORC-OUT):

     An outbound EHR request is received.
     An EHR request message is constructed and sent to the system.
     The system processes the EHR request message.
     The system sends the patient's EHR to the requesting practice.
     The system verifies that the outgoing EHR core matches the original large EHR core.
     A continue request is sent to the system.
     The system verifies that the received EHR fragments match the original large EHR fragments.
     Overall, this test ensures that a large EHR can be successfully transferred into and out of the system, with the integrity of the data maintained throughout the process
     */
    @Test
    void shouldTransferALargeEHRInAndOut() {
        final String nhsNumber = PATIENT_WITH_SMALL_EHR_IN_REPO_AND_MOF_SET_TO_TPP.nhsNumber();
        // For readability sake, it is easiest to set up test so that sender & recipient are the same practice
        final String recipientOdsCode = TPP_PTL_INT.odsCode();
        final String repositoryOdsCode = nhsProperties.getRepoOdsCode();

        log.info("nhsNumber: " + nhsNumber);
        log.info("recipientOdsCode: " + recipientOdsCode);
        log.info("repositoryOdsCode: " + repositoryOdsCode);

        /*
        ORC-IN
         */

        // Given a large EHR is transferred into the repository

        // create entry in transfer tracker db with status INBOUND_REQUEST_SENT
        this.transferTrackerService.saveConversation(ConversationRecord.builder()
                .inboundConversationId(inboundConversationId)
                .nhsNumber(nhsNumber)
                .transferStatus(INBOUND_REQUEST_SENT.name())
                .nemsMessageId(nemsMessageId)
                .sourceGp(senderOdsCode)
                .associatedTest(testName)
                .build()
        );

        // Construct large EHR core message
        LargeEhrCoreTemplateContext largeEhrCoreTemplateContext = LargeEhrCoreTemplateContext.builder()
                .inboundConversationId(inboundConversationId)
                .largeEhrCoreMessageId(largeEhrCoreMessageId)
                .fragmentMessageId(fragment1MessageId)
                .nhsNumber(nhsNumber)
                .senderOdsCode(senderOdsCode)
                .build();
        String largeEhrCore = this.templatingService.getTemplatedString(TemplateVariant.LARGE_EHR_CORE, largeEhrCoreTemplateContext);

        // Put the large EHR core message onto the mhsInboundQueue
        mhsInboundQueue.sendMessage(largeEhrCore, inboundConversationId);
        log.info("added EHR IN message containing large EHR core to mhsInboundQueue");
        log.info("conversationId {} exists in transferTrackerDb: {}", inboundConversationId, transferTrackerService.inboundConversationIdExists(inboundConversationId));

        // Wait until the large EHR core is processed by the ehr-transfer-service
        String status = transferTrackerService.waitForConversationTransferStatusMatching(inboundConversationId, INBOUND_CONTINUE_REQUEST_SENT.name());
        log.info("tracker db status: {}", status);

        // Construct large EHR fragment messages
        List<String> largeEhrFragments = this.templatingService.getMultipleTemplatedStrings(Map.of(
                TemplateVariant.LARGE_EHR_FRAGMENT_WITH_REF, LargeEhrFragmentWithReferencesContext.builder()
                        .inboundConversationId(inboundConversationId)
                        .fragmentMessageId(fragment1MessageId)
                        .fragmentTwoMessageId(fragment2MessageId)
                        .recipientOdsCode(recipientOdsCode)
                        .senderOdsCode(repositoryOdsCode)
                        .build(),

                TemplateVariant.LARGE_EHR_FRAGMENT_NO_REF, LargeEhrFragmentNoReferencesContext.builder()
                        .inboundConversationId(inboundConversationId)
                        .fragmentMessageId(fragment2MessageId)
                        .recipientOdsCode(recipientOdsCode)
                        .senderOdsCode(repositoryOdsCode)
                        .build()
        ));

        // Put the large fragment messages onto the mhsInboundQueue
        largeEhrFragments.forEach(fragment -> mhsInboundQueue.sendMessage(fragment, inboundConversationId));
        log.info("added EHR IN messages containing large EHR fragments to mhsInboundQueue");

        // Wait until the patient EHR is successfully transferred to the repository
        log.info("Waiting for transferTrackerDb status of INBOUND_COMPLETE");
        status = transferTrackerService.waitForConversationTransferStatusMatching(inboundConversationId, INBOUND_COMPLETE.name());
        log.info("tracker db status: {}", status);

        /*
        ORC-OUT
         */

        // When an EHR out request is received

        // Construct an EHR out request
        EhrRequestTemplateContext ehrRequestTemplateContext = EhrRequestTemplateContext.builder()
                .outboundConversationId(outboundConversationId)
                .nhsNumber(nhsNumber)
                .senderOdsCode(senderOdsCode)
                .asidCode(asidCode)
                .build();
        String ehrRequestMessage = this.templatingService.getTemplatedString(EHR_REQUEST, ehrRequestTemplateContext);

        // Add EHR out request to mhsInboundQueue
        mhsInboundQueue.sendMessage(ehrRequestMessage, outboundConversationId);
        log.info("added EHR OUT request to mhsInboundQueue");

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
                        .senderOdsCode(senderOdsCode)
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

    @Test
    void shouldTransferOutASmallEhrWhenThePatientIsSoftDeleted() {
        // given
        final String nhsNumber = PATIENT_WITH_SMALL_EHR_IN_REPO_AND_MOF_SET_TO_TPP.nhsNumber();
        final Instant deletedAt = Instant.now().plus(30, ChronoUnit.DAYS);

        // template context set-up and templating
        final SmallEhrTemplateContext smallEhrContext = SmallEhrTemplateContext.builder()
            .nhsNumber(nhsNumber)
            .inboundConversationId(inboundConversationId)
            .messageId(messageId)
            .build();

        final EhrRequestTemplateContext ehrRequestContext = EhrRequestTemplateContext.builder()
            .outboundConversationId(outboundConversationId)
            .nhsNumber(nhsNumber)
            .senderOdsCode(senderOdsCode)
            .asidCode(asidCode)
            .build();

        final String smallEhr = templatingService.getTemplatedString(SMALL_EHR, smallEhrContext);
        final String ehrRequest = templatingService.getTemplatedString(EHR_REQUEST, ehrRequestContext);

        // when
        transferTrackerService.saveConversation(ConversationRecord.builder()
            .inboundConversationId(inboundConversationId)
            .nhsNumber(nhsNumber)
            .transferStatus(INBOUND_REQUEST_SENT.name())
            .nemsMessageId(nemsMessageId)
            .sourceGp(senderOdsCode)
            .associatedTest(testName)
            .build()
        );

        mhsInboundQueue.sendMessage(smallEhr, inboundConversationId);
        transferTrackerService.waitForConversationTransferStatusMatching(inboundConversationId, INBOUND_COMPLETE.name());
        transferTrackerService.softDeleteSmallEhr(inboundConversationId, deletedAt);
        mhsInboundQueue.sendMessage(ehrRequest, outboundConversationId);

        // then
        final List<SqsMessage> messages = gp2gpMessengerOQ.getAllMessagesContaining(EHR_CORE.interactionId, 1).stream()
            .filter(message -> message.contains(outboundConversationId))
            .toList();

        assertThat(messages.size()).isEqualTo(1);
        assertTrue(transferTrackerService.verifyConversationContainsOutboundConversationId(
            inboundConversationId,
            outboundConversationId
        ));
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
        final String nhsNumber = PATIENT_WITH_SMALL_EHR_IN_REPO_AND_MOF_SET_TO_TPP.nhsNumber();
        log.info("nhsNumber: " + nhsNumber);

        this.repoService.addSmallEhrToEhrRepo(SMALL_EHR_WITH_99_ATTACHMENTS, nhsNumber);

        // When an EHR out request is received
        EhrRequestTemplateContext templateContext = EhrRequestTemplateContext.builder()
                .nhsNumber(nhsNumber)
                .senderOdsCode(senderOdsCode)
                .asidCode(asidCode)
                .build();

        String ehrRequestMessage = this.templatingService.getTemplatedString(EHR_REQUEST, templateContext);
        String conversationId = templateContext.getOutboundConversationId();

        mhsInboundQueue.sendMessage(ehrRequestMessage, conversationId);

        // Then the patient EHR is transferred to the requesting practice
        boolean messageFound = this.gp2gpMessengerOQ.getAllMessagesFromQueueWithConversationIds(1, 0,
                List.of(conversationId));

        // then
        assertTrue(messageFound);
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



    @Test
    void shouldPutASmallEHROntoRepoAndSendEHRToMHSOutboundWhenReceivingRequestFromGP() {
        // Given
        final String nhsNumber = PATIENT_WITH_SMALL_EHR_IN_REPO_AND_MOF_SET_TO_TPP.nhsNumber();
        log.info("nhsNumber: " + nhsNumber);

        String smallEhr = templatingService.getTemplatedString(SMALL_EHR, SmallEhrTemplateContext.builder()
                .inboundConversationId(inboundConversationId)
                .nhsNumber(nhsNumber)
                .build());

        String ehrRequest = templatingService.getTemplatedString(EHR_REQUEST, EhrRequestTemplateContext.builder()
                .nhsNumber(nhsNumber)
                .outboundConversationId(outboundConversationId)
                .senderOdsCode(senderOdsCode)
                .asidCode(asidCode)
                .build());

        // When
        // change transfer db status to ACTION:EHR_REQUEST_SENT before putting on inbound queue
        transferTrackerService.saveConversation(ConversationRecord.builder()
            .inboundConversationId(inboundConversationId)
            .nemsMessageId(nemsMessageId)
            .nhsNumber(nhsNumber)
            .sourceGp(senderOdsCode)
            .transferStatus(INBOUND_REQUEST_SENT.name())
            .associatedTest(testName)
            .build());

        // Put the patient into mhsInboundQueue as a UK05 message
        mhsInboundQueue.sendMessage(smallEhr, inboundConversationId);

        log.info("conversationId: {}, Exists: {}", inboundConversationId, transferTrackerService.inboundConversationIdExists(inboundConversationId));

        String status = transferTrackerService.waitForConversationTransferStatusMatching(
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
        final String nhsNumber = "9727018157"; // existed on old version of test, what is this number?
        final String senderOdsCode = TPP_PTL_INT.odsCode();
        final String recipientOdsCode = EMIS_PTL_INT.odsCode();
        final String repositoryOdsCode = nhsProperties.getRepoOdsCode();
        final String asidCode = TPP_PTL_INT.asidCode();

        log.info("nhsNumber: " + nhsNumber);
        log.info("senderOdsCode: " + senderOdsCode);
        log.info("recipientOdsCode: " + recipientOdsCode);
        log.info("repositoryOdsCode: " + repositoryOdsCode);
        log.info("asidCode: " + asidCode);

        String largeEhrCore = this.templatingService.getTemplatedString(LARGE_EHR_CORE,
                LargeEhrCoreTemplateContext.builder()
                        .inboundConversationId(inboundConversationId)
                        .largeEhrCoreMessageId(largeEhrCoreMessageId)
                        .fragmentMessageId(fragment1MessageId)
                        .nhsNumber(nhsNumber)
                        .senderOdsCode(senderOdsCode)
                        .build());

        String largeEhrFragment1 = this.templatingService.getTemplatedString(LARGE_EHR_FRAGMENT_WITH_REF,
                LargeEhrFragmentWithReferencesContext.builder()
                        .inboundConversationId(inboundConversationId)
                        .fragmentMessageId(fragment1MessageId)
                        .fragmentTwoMessageId(fragment2MessageId)
                        .recipientOdsCode(recipientOdsCode)
                        .senderOdsCode(repositoryOdsCode)
                        .build());

        String largeEhrFragment2 = this.templatingService.getTemplatedString(LARGE_EHR_FRAGMENT_NO_REF,
                LargeEhrFragmentNoReferencesContext.builder()
                        .inboundConversationId(inboundConversationId)
                        .fragmentMessageId(fragment2MessageId)
                        .recipientOdsCode(recipientOdsCode)
                        .senderOdsCode(repositoryOdsCode)
                        .build());

        String ehrRequest = this.templatingService.getTemplatedString(EHR_REQUEST,
                EhrRequestTemplateContext.builder()
                        .outboundConversationId(outboundConversationId)
                        .nhsNumber(nhsNumber)
                        .senderOdsCode(senderOdsCode)
                        .asidCode(asidCode)
                        .build());

        String continueRequest = this.templatingService.getTemplatedString(CONTINUE_REQUEST,
                ContinueRequestTemplateContext.builder()
                        .outboundConversationId(outboundConversationId)
                        .recipientOdsCode(recipientOdsCode)
                        .senderOdsCode(senderOdsCode)
                        .build());

        transferTrackerService.saveConversation(ConversationRecord.builder()
                .inboundConversationId(inboundConversationId)
                .nemsMessageId(nemsMessageId)
                .nhsNumber(nhsNumber)
                .sourceGp(senderOdsCode)
                .transferStatus(INBOUND_REQUEST_SENT.name())
                .associatedTest(testName)
                .build());
        
        // when
        mhsInboundQueue.sendMessage(largeEhrCore, inboundConversationId);

        log.info("conversationIdExists: {}", transferTrackerService.inboundConversationIdExists(inboundConversationId));
        String status = transferTrackerService.waitForConversationTransferStatusMatching(inboundConversationId, INBOUND_CONTINUE_REQUEST_SENT.name());
        log.info("tracker db status: {}", status);

        mhsInboundQueue.sendMessage(largeEhrFragment1, inboundConversationId);
        mhsInboundQueue.sendMessage(largeEhrFragment2, inboundConversationId);

        status = transferTrackerService.waitForConversationTransferStatusMatching(inboundConversationId, INBOUND_COMPLETE.name());
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
