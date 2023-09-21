package uk.nhs.prm.e2etests.test;

import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.xmlunit.diff.Diff;

import uk.nhs.prm.e2etests.enumeration.*;
import uk.nhs.prm.e2etests.model.RepoIncomingMessage;
import uk.nhs.prm.e2etests.model.RepoIncomingMessageBuilder;
import uk.nhs.prm.e2etests.model.SqsMessage;
import uk.nhs.prm.e2etests.model.database.Acknowledgement;
import uk.nhs.prm.e2etests.model.database.TransferTrackerRecord;
import uk.nhs.prm.e2etests.model.response.PdsAdaptorResponse;
import uk.nhs.prm.e2etests.model.templatecontext.*;
import uk.nhs.prm.e2etests.property.Gp2gpMessengerProperties;
import uk.nhs.prm.e2etests.property.NhsProperties;
import uk.nhs.prm.e2etests.queue.SimpleAmqpQueue;
import uk.nhs.prm.e2etests.queue.ehrtransfer.EhrTransferServiceParsingDeadLetterQueue;
import uk.nhs.prm.e2etests.queue.ehrtransfer.EhrTransferServiceRepoIncomingQueue;
import uk.nhs.prm.e2etests.queue.ehrtransfer.observability.EhrTransferServiceEhrCompleteOQ;
import uk.nhs.prm.e2etests.queue.ehrtransfer.observability.EhrTransferServiceLargeEhrFragmentsOQ;
import uk.nhs.prm.e2etests.queue.ehrtransfer.observability.EhrTransferServiceLargeEhrOQ;
import uk.nhs.prm.e2etests.queue.ehrtransfer.observability.EhrTransferServiceNegativeAcknowledgementOQ;
import uk.nhs.prm.e2etests.queue.ehrtransfer.observability.EhrTransferServiceSmallEhrOQ;
import uk.nhs.prm.e2etests.queue.ehrtransfer.observability.EhrTransferServiceTransferCompleteOQ;
import uk.nhs.prm.e2etests.queue.ehrtransfer.observability.EhrTransferServiceUnhandledOQ;
import uk.nhs.prm.e2etests.queue.gp2gpmessenger.observability.Gp2GpMessengerOQ;
import uk.nhs.prm.e2etests.service.*;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.util.AssertionErrors.assertFalse;

import static uk.nhs.prm.e2etests.enumeration.Gp2GpSystem.EMIS_PTL_INT;
import static uk.nhs.prm.e2etests.enumeration.Gp2GpSystem.TPP_PTL_INT;
import static uk.nhs.prm.e2etests.enumeration.MessageType.*;
import static uk.nhs.prm.e2etests.enumeration.TemplateVariant.*;
import static uk.nhs.prm.e2etests.enumeration.TransferTrackerStatus.EHR_REQUEST_SENT;
import static uk.nhs.prm.e2etests.enumeration.TransferTrackerStatus.EHR_TRANSFER_TO_REPO_COMPLETE;
import static uk.nhs.prm.e2etests.utility.ThreadUtility.sleepFor;
import static uk.nhs.prm.e2etests.enumeration.TemplateVariant.CONTINUE_REQUEST;
import static uk.nhs.prm.e2etests.enumeration.TemplateVariant.EHR_REQUEST;
import static uk.nhs.prm.e2etests.enumeration.TemplateVariant.SMALL_EHR;
import static uk.nhs.prm.e2etests.enumeration.TransferTrackerStatus.*;
import static uk.nhs.prm.e2etests.utility.XmlComparisonUtility.comparePayloads;
import static uk.nhs.prm.e2etests.utility.XmlComparisonUtility.getPayloadOptional;
import static uk.nhs.prm.e2etests.utility.TestDataUtility.randomNemsMessageId;

@Log4j2
@SpringBootTest
@ExtendWith(ForceXercesParserSoLogbackDoesNotBlowUpWhenUsingSwiftMqClient.class)
@TestPropertySource(properties = {"test.pds.username=e2e-test"})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RepositoryE2ETest {
    private final RepoService repoService;
    private final EhrOutService ehrOutService;
    private final TransferTrackerService transferTrackerService;
    private final PdsAdaptorService pdsAdaptorService;
    private final TemplatingService templatingService;
    private final HealthCheckService healthCheckService;
    private final SimpleAmqpQueue mhsInboundQueue;
    private final Gp2GpMessengerOQ gp2gpMessengerOQ;
    private final EhrTransferServiceTransferCompleteOQ ehrTransferServiceTransferCompleteOQ;
    private final EhrTransferServiceUnhandledOQ ehrTransferServiceUnhandledOQ;
    private final EhrTransferServiceEhrCompleteOQ ehrTransferServiceEhrCompleteOQ;
    private final EhrTransferServiceLargeEhrFragmentsOQ ehrTransferServiceLargeEhrFragmentsOQ;
    private final EhrTransferServiceRepoIncomingQueue ehrTransferServiceRepoIncomingQueue;
    private final EhrTransferServiceSmallEhrOQ ehrTransferServiceSmallEhrOQ;
    private final EhrTransferServiceLargeEhrOQ ehrTransferServiceLargeEhrOQ;
    private final EhrTransferServiceNegativeAcknowledgementOQ ehrTransferServiceNegativeAcknowledgementOQ;
    private final EhrTransferServiceParsingDeadLetterQueue ehrTransferServiceParsingDeadLetterQueue;
    private final Gp2gpMessengerProperties gp2GpMessengerProperties;
    private final NhsProperties nhsProperties;

    // Constants
    private static final int EHR_REQUEST_MHS_INBOUND_TIMEOUT_MILLISECONDS = 500;
    private static final int CONTINUE_REQUEST_MHS_INBOUND_TIMEOUT_MILLISECONDS = 3000;

    @Autowired
    public RepositoryE2ETest(
            RepoService repoService,
            EhrOutService ehrOutService,
            TransferTrackerService transferTrackerService,
            PdsAdaptorService pdsAdaptorService,
            TemplatingService templatingService,
            HealthCheckService healthCheckService,
            SimpleAmqpQueue mhsInboundQueue,
            Gp2GpMessengerOQ gp2gpMessengerOQ,
            EhrTransferServiceTransferCompleteOQ ehrTransferServiceTransferCompleteOQ,
            EhrTransferServiceUnhandledOQ ehrTransferServiceUnhandledOQ,
            EhrTransferServiceEhrCompleteOQ ehrTransferServiceEhrCompleteOQ,
            EhrTransferServiceLargeEhrFragmentsOQ ehrTransferServiceLargeEhrFragmentsOQ,
            EhrTransferServiceRepoIncomingQueue ehrTransferServiceRepoIncomingQueue,
            EhrTransferServiceSmallEhrOQ ehrTransferServiceSmallEhrOQ,
            EhrTransferServiceLargeEhrOQ ehrTransferServiceLargeEhrOQ,
            EhrTransferServiceNegativeAcknowledgementOQ ehrTransferServiceNegativeAcknowledgementOQ,
            EhrTransferServiceParsingDeadLetterQueue ehrTransferServiceParsingDeadLetterQueue,
            Gp2gpMessengerProperties gp2GpMessengerProperties,
            NhsProperties nhsProperties
    ) {
        this.repoService = repoService;
        this.ehrOutService = ehrOutService;
        this.transferTrackerService = transferTrackerService;
        this.pdsAdaptorService = pdsAdaptorService;
        this.templatingService = templatingService;
        this.healthCheckService = healthCheckService;
        this.mhsInboundQueue = mhsInboundQueue;
        this.gp2gpMessengerOQ = gp2gpMessengerOQ;
        this.ehrTransferServiceTransferCompleteOQ = ehrTransferServiceTransferCompleteOQ;
        this.ehrTransferServiceUnhandledOQ = ehrTransferServiceUnhandledOQ;
        this.ehrTransferServiceEhrCompleteOQ = ehrTransferServiceEhrCompleteOQ;
        this.ehrTransferServiceLargeEhrFragmentsOQ = ehrTransferServiceLargeEhrFragmentsOQ;
        this.ehrTransferServiceRepoIncomingQueue = ehrTransferServiceRepoIncomingQueue;
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

    // The following test should eventually test that we can send a small EHR - until we have an EHR in repo/test patient ready to send,
    // we are temporarily doing a smaller test to cover from amqp -> ehr out queue
    @Test
    @EnabledIfEnvironmentVariable(named = "NHS_ENVIRONMENT", matches = "dev")
    void shouldIdentifyEhrRequestAsEhrOutMessage() {
        // given
        final String outboundConversationId = UUID.randomUUID().toString();
        final String nhsNumber = "9727018440";
        final String sendingOdsCode = "B85002";

        final String ehrRequestMessage = this.templatingService.getTemplatedString(EHR_REQUEST, EhrRequestTemplateContext.builder()
                .messageId(UUID.randomUUID().toString())
                .sendingOdsCode(sendingOdsCode)
                .nhsNumber(nhsNumber)
                .outboundConversationId(outboundConversationId)
                .build());

        // when
        mhsInboundQueue.sendMessage(ehrRequestMessage, outboundConversationId);

        // then
        assertThat(ehrTransferServiceUnhandledOQ.getMessageContaining(ehrRequestMessage)).isNotNull();
    }

    @Test
    void shouldVerifyThatASmallEhrXMLIsUnchanged() {
        // given
        String inboundConversationId = UUID.randomUUID().toString();
        String outboundConversationId = UUID.randomUUID().toString();
        String nhsNumber = "9727018440";
        String sendingOdsCode = "M85019";
        String asidCode = "200000000149";

        SmallEhrTemplateContext smallEhrTemplateContext = SmallEhrTemplateContext.builder()
                .inboundConversationId(inboundConversationId.toUpperCase())
                .nhsNumber(nhsNumber)
                .build();

        EhrRequestTemplateContext ehrRequestTemplateContext = EhrRequestTemplateContext
                .builder()
                .outboundConversationId(outboundConversationId.toUpperCase())
                .nhsNumber(nhsNumber)
                .sendingOdsCode(sendingOdsCode)
                .asidCode(asidCode)
                .build();

        String smallEhrMessage = this.templatingService.getTemplatedString(TemplateVariant.SMALL_EHR_WITHOUT_LINEBREAKS, smallEhrTemplateContext);
        String ehrRequestMessage = this.templatingService.getTemplatedString(EHR_REQUEST, ehrRequestTemplateContext);

        // When
        // change transfer db status to ACTION:EHR_REQUEST_SENT before putting on inbound queue
        // Put the patient into inboundQueueFromMhs as a UK05 message
        this.transferTrackerService.save(TransferTrackerRecord.builder()
                .conversationId(inboundConversationId)
                .largeEhrCoreMessageId("")
                .nemsMessageId(randomNemsMessageId())
                .nhsNumber(nhsNumber)
                .sourceGp(sendingOdsCode)
                .state(EHR_REQUEST_SENT.status)
                .build()
        );
        mhsInboundQueue.sendMessage(smallEhrMessage, inboundConversationId);


        log.info("inbound conversationId: {}", inboundConversationId);
        log.info("conversationIdExists: {}", transferTrackerService.conversationIdExists(inboundConversationId));
        String status = transferTrackerService.waitForStatusMatching(inboundConversationId, EHR_TRANSFER_TO_REPO_COMPLETE.status);
        log.info("tracker db status: {}", status);


        // Put a EHR request to inboundQueueFromMhs
        mhsInboundQueue.sendMessage(ehrRequestMessage, outboundConversationId);

        // Then
        // assert gp2gpMessenger queue got a message of UK06
        SqsMessage gp2gpMessage = gp2gpMessengerOQ.getMessageContaining(outboundConversationId);

        assertThat(gp2gpMessage).isNotNull();
        assertTrue(gp2gpMessage.contains(EHR_CORE.interactionId));

        String gp2gpMessengerPayload = getPayloadOptional(gp2gpMessage.getBody()).orElseThrow();
        String smallEhrPayload = getPayloadOptional(smallEhrMessage).orElseThrow();
        log.info("Payload from gp2gpMessenger: {}", gp2gpMessengerPayload);
        log.info("Payload from smallEhr: {}", smallEhrPayload);

        Diff myDiff = comparePayloads(gp2gpMessengerPayload, smallEhrPayload);

        assertFalse(myDiff.toString(), myDiff.hasDifferences());
    }

    @Test
    @Disabled("This test was failing before refactoring. To be fixed after refactoring is complete.")
    void shouldVerifyThatALargeEhrXMLIsUnchanged() {
        // given
        String inboundConversationId = UUID.randomUUID().toString();
        String outboundConversationId = UUID.randomUUID().toString();

        String largeEhrCoreMessageId = UUID.randomUUID().toString();
        String fragment1MessageId = UUID.randomUUID().toString();
        String fragment2MessageId = UUID.randomUUID().toString();
        String nhsNumber = "9727018157";
        String sendingOdsCode = "N82668";
//        String newGpForTestPatient = "M85019";

        // Get the templates.
        String ehrRequestMessage = this.templatingService.getTemplatedString(EHR_REQUEST, EhrRequestTemplateContext.builder()
                .outboundConversationId(outboundConversationId)
                .nhsNumber(nhsNumber)
                .sendingOdsCode(sendingOdsCode)
                .build());

        String continueRequestMessage = this.templatingService.getTemplatedString(
                CONTINUE_REQUEST,
                ContinueRequestTemplateContext.builder()
                    .outboundConversationId(outboundConversationId)
                    .build());

        String largeEhrCore = this.templatingService.getTemplatedString(TemplateVariant.LARGE_EHR_CORE, LargeEhrCoreTemplateContext
                .builder()
                .build());

        List<String> largeEhrFragments = this.templatingService.getMultipleTemplatedStrings(Map.of(
                TemplateVariant.LARGE_EHR_FRAGMENT_WITH_REF, LargeEhrFragmentWithReferencesContext.builder().build(),
                TemplateVariant.LARGE_EHR_FRAGMENT_NO_REF, LargeEhrFragmentNoReferencesContext.builder().build()
        ));

        this.transferTrackerService.save(TransferTrackerRecord.builder()
                .conversationId(inboundConversationId)
                .largeEhrCoreMessageId(largeEhrCoreMessageId)
                .nemsMessageId(randomNemsMessageId())
                .nhsNumber(nhsNumber)
                .sourceGp(sendingOdsCode)
                .state(EHR_REQUEST_SENT.status)
                .build()
        );

        // when
        mhsInboundQueue.sendMessage(largeEhrCore, inboundConversationId);
        log.info("conversationIdExists: {}", transferTrackerService.conversationIdExists(inboundConversationId));
        String status = transferTrackerService.waitForStatusMatching(inboundConversationId, TransferTrackerStatus.LARGE_EHR_CONTINUE_REQUEST_SENT.status);
        log.info("tracker db status: {}", status);

        log.info("fragment 1 message id: {}", fragment1MessageId);
        log.info("fragment 2 message id: {}", fragment2MessageId);

         largeEhrFragments.forEach(fragment -> mhsInboundQueue.sendMessage(fragment, inboundConversationId));

        status = transferTrackerService.waitForStatusMatching(inboundConversationId, EHR_TRANSFER_TO_REPO_COMPLETE.status);
        log.info("tracker db status: {}", status);

        // Put a EHR request to inboundQueueFromMhs
        mhsInboundQueue.sendMessage(ehrRequestMessage, outboundConversationId);

        // Then
        // assert gp2gpMessenger queue got a message of UK06
        SqsMessage gp2gpMessageUK06 = gp2gpMessengerOQ.getMessageContaining(outboundConversationId);

        assertThat(gp2gpMessageUK06).isNotNull();
        assertTrue(gp2gpMessageUK06.contains(EHR_CORE.interactionId));

        String gp2gpMessengerEhrCorePayload = getPayloadOptional(gp2gpMessageUK06.getBody()).orElseThrow();
        String largeEhrCorePayload = getPayloadOptional(largeEhrCore).orElseThrow();

        Diff compareEhrCores = comparePayloads(gp2gpMessengerEhrCorePayload, largeEhrCorePayload);
        boolean ehrCoreIsIdentical = !compareEhrCores.hasDifferences();
        assertTrue(ehrCoreIsIdentical);

        // Put a continue request to inboundQueueFromMhs
        mhsInboundQueue.sendMessage(
                continueRequestMessage,
                outboundConversationId
        );

        // get all message fragments from gp2gp-messenger observability queue and compare with inbound fragments
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
        // The builder by default already generates a random conversation ID, which fulfills the test condition
        ContinueRequestTemplateContext continueRequestContext = ContinueRequestTemplateContext.builder()
                .recipientOdsCode(TPP_PTL_INT.odsCode())
                .senderOdsCode(EMIS_PTL_INT.odsCode()).build();

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

    @ParameterizedTest(name = "[{index}] Case of {0}")
    @MethodSource("erroneousInboundMessages")
    @DisplayName("Test how ORC handles Erroneous inbound messages")
    void testsWithErroneousInboundMessages(String inboundMessage, String conversationId) {
        // when
        mhsInboundQueue.sendMessage(inboundMessage, conversationId.toLowerCase());

        // then
        log.info("Verify that EHR Transfer Service put the erroneous inbound message to unhandled queue");
        SqsMessage unhandledMessage = ehrTransferServiceUnhandledOQ.getMessageContaining(conversationId);
        assertThat(unhandledMessage.getBody()).isEqualTo(inboundMessage);

        log.info("Verify that no response message with given conversation id is on the gp2gp observability queue");
        // later this could be changed to asserting an NACK message on the queue if we do send back NACKs
        assertTrue(gp2gpMessengerOQ.verifyNoMessageContaining(conversationId));

        assertTrue(healthCheckService.healthCheckAllPassing());
    }

    // End of tests for Erroneous Inbound messages

    @Test
    @Disabled("This test was failing before refactoring. The cause seems to be related to EMIS instance not working")
    void shouldReceivingAndTrackAllLargeEhrFragments_DevAndTest() {
        Patient largeEhrAtEmisWithRepoMof = Patient.largeEhrAtEmisWithRepoMof(this.nhsProperties.getNhsEnvironment());

        setManagingOrganisationToRepo(largeEhrAtEmisWithRepoMof.nhsNumber());

        RepoIncomingMessage triggerMessage = new RepoIncomingMessageBuilder()
                .withPatient(largeEhrAtEmisWithRepoMof)
                .withEhrSourceGp(EMIS_PTL_INT)
                .withEhrDestinationAsRepo(nhsProperties.getNhsEnvironment())
                .build();

        ehrTransferServiceRepoIncomingQueue.send(triggerMessage);
        assertThat(ehrTransferServiceEhrCompleteOQ.getMessageContaining(triggerMessage.getConversationId())).isNotNull();
        assertTrue(transferTrackerService.isStatusForConversationIdPresent(triggerMessage.getConversationId(), EHR_TRANSFER_TO_REPO_COMPLETE.status));
    }

    @ParameterizedTest
    @MethodSource("largeEhrScenariosRunningOnCommit_ButNotEmisWhichIsCurrentlyHavingIssues")
    @EnabledIfEnvironmentVariable(named = "NHS_ENVIRONMENT", matches = "dev", disabledReason = "We have only one set of variants for large ehr")
    void shouldTransferRepresentativeSizesAndTypesOfEhrs_DevOnly(Gp2GpSystem sourceSystem, LargeEhrVariant largeEhr) {
        RepoIncomingMessage triggerMessage = new RepoIncomingMessageBuilder()
                .withPatient(largeEhr.patient())
                .withEhrSourceGp(sourceSystem)
                .withEhrDestinationAsRepo(nhsProperties.getNhsEnvironment())
                .build();

        setManagingOrganisationToRepo(largeEhr.patient().nhsNumber());

        ehrTransferServiceRepoIncomingQueue.send(triggerMessage);

        assertThat(ehrTransferServiceTransferCompleteOQ.getMessageContainingAttribute(
                "conversationId",
                triggerMessage.getConversationId(),
                largeEhr.timeoutMinutes(),
                TimeUnit.MINUTES))
                .isNotNull();

        assertTrue(transferTrackerService.isStatusForConversationIdPresent(triggerMessage.getConversationId(), EHR_TRANSFER_TO_REPO_COMPLETE.status));
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 4, 6})
    void shouldRejectDuplicatedEhrRequestMessagesFromTheSameGP(int numberOfEhrRequests) {
        // given
        String nhsNumber = Patient.PATIENT_WITH_SMALL_EHR_IN_REPO_AND_MOF_SET_TO_TPP.nhsNumber();

        EhrRequestTemplateContext templateContext = EhrRequestTemplateContext.builder()
                .nhsNumber(nhsNumber)
                .sendingOdsCode(TPP_PTL_INT.odsCode())
                .asidCode(TPP_PTL_INT.asidCode()).build();

        String ehrRequestMessage = this.templatingService.getTemplatedString(EHR_REQUEST, templateContext);
        String conversationId = templateContext.getOutboundConversationId();

        // when
        repoService.addSmallEhrToEhrRepo(nhsNumber, SMALL_EHR);

        for(int i = 0; i < numberOfEhrRequests; i++) {
            mhsInboundQueue.sendMessage(ehrRequestMessage, conversationId);
            log.info("Duplicate EHR Request {} of {} sent to MHS Inbound queue successfully.", (i + 1), numberOfEhrRequests);
        }

        boolean messagesFound = this.gp2gpMessengerOQ.getAllMessagesFromQueueWithConversationIds(1, 0,
                List.of(conversationId));

        // then
        assertTrue(messagesFound);
    }

    @Test
    void shouldSuccessfullyParseASmallEhrWith99Attachments() {
        // given
        String nhsNumber = Patient.PATIENT_WITH_SMALL_EHR_IN_REPO_AND_MOF_SET_TO_TPP.nhsNumber();
        EhrRequestTemplateContext templateContext = EhrRequestTemplateContext.builder()
                .nhsNumber(nhsNumber)
                .sendingOdsCode(TPP_PTL_INT.odsCode())
                .asidCode(TPP_PTL_INT.asidCode()).build();
        String ehrRequestMessage = this.templatingService.getTemplatedString(EHR_REQUEST, templateContext);
        String conversationId = templateContext.getOutboundConversationId();

        // when
        this.repoService.addSmallEhrToEhrRepo(nhsNumber, SMALL_EHR_WITH_99_ATTACHMENTS);

        mhsInboundQueue.sendMessage(ehrRequestMessage, conversationId);

        boolean messageFound = this.gp2gpMessengerOQ.getAllMessagesFromQueueWithConversationIds(1, 0,
                List.of(conversationId));

        // then
        assertTrue(messageFound);
    }
  
    private static Stream<Arguments> largeEhrScenariosRunningOnCommit_ButNotEmisWhichIsCurrentlyHavingIssues() {
        return largeEhrScenariosRunningOnCommit().filter(args ->
                args.get()[0] != EMIS_PTL_INT);
    }

    // TODO: ABSTRACT THIS OUT TO ANOTHER CLASS
    private static Stream<Arguments> largeEhrScenariosRunningOnCommit() {
        return Stream.of(
                Arguments.of(EMIS_PTL_INT, LargeEhrVariant.SINGLE_LARGE_FRAGMENT),
                Arguments.of(Gp2GpSystem.TPP_PTL_INT, LargeEhrVariant.SINGLE_LARGE_FRAGMENT),
                Arguments.of(EMIS_PTL_INT, LargeEhrVariant.LARGE_MEDICAL_HISTORY),
                Arguments.of(Gp2GpSystem.TPP_PTL_INT, LargeEhrVariant.LARGE_MEDICAL_HISTORY),
                Arguments.of(EMIS_PTL_INT, LargeEhrVariant.MULTIPLE_LARGE_FRAGMENTS),
                Arguments.of(Gp2GpSystem.TPP_PTL_INT, LargeEhrVariant.MULTIPLE_LARGE_FRAGMENTS)
        );
    }

    @ParameterizedTest
    @MethodSource("largeEhrScenariosToBeRunAsRequired")
    @EnabledIfEnvironmentVariable(named = "NHS_ENVIRONMENT", matches = "dev", disabledReason = "We have only one set of variants for large ehr")
    @EnabledIfEnvironmentVariable(named = "RUN_ALL_VARIANTS", matches = "true", disabledReason = "Too slow / problematic for on-commit run")
    void shouldTransferRemainingSizesAndTypesOfEhrs_DevOnly(Gp2GpSystem sourceSystem, LargeEhrVariant largeEhr) {
        RepoIncomingMessage triggerMessage = new RepoIncomingMessageBuilder()
                .withPatient(largeEhr.patient())
                .withEhrSourceGp(sourceSystem)
                .withEhrDestinationAsRepo(nhsProperties.getNhsEnvironment())
                .build();

        setManagingOrganisationToRepo(largeEhr.patient().nhsNumber());

        ehrTransferServiceRepoIncomingQueue.send(triggerMessage);

        assertThat(ehrTransferServiceTransferCompleteOQ.getMessageContainingAttribute(
                "conversationId",
                triggerMessage.getConversationId(),
                largeEhr.timeoutMinutes(),
                TimeUnit.MINUTES))
                .isNotNull();

        assertTrue(transferTrackerService.isStatusForConversationIdPresent(triggerMessage.getConversationId(), EHR_TRANSFER_TO_REPO_COMPLETE.status));
    }

    // TODO: ABSTRACT THIS OUT TO ANOTHER CLASS
    private static Stream<Arguments> largeEhrScenariosToBeRunAsRequired() {
        return Stream.of(
                // 5mins+ variation -> removed from regression as intermittently takes 2+ hours
                // to complete which, while successful, is not sufficiently timely for on-commit regression
                Arguments.of(EMIS_PTL_INT, LargeEhrVariant.HIGH_FRAGMENT_COUNT),
                Arguments.of(Gp2GpSystem.TPP_PTL_INT, LargeEhrVariant.HIGH_FRAGMENT_COUNT),

                // 20mins+, filling FSS disks causing outages -> to be run ad hoc as needed
                 Arguments.of(EMIS_PTL_INT, LargeEhrVariant.SUPER_LARGE)

                // could not move it EMIS to TPP - Large Message general failure
                // need to establish current TPP limits that are applying in this case
                // Arguments.of(Gp2GpSystem.TPP_PTL_INT, LargeEhrVariant.SUPER_LARGE)
        );
    }

    @Disabled("Running manually in dev environment.")
    @Test
    void shouldHandleMultipleEhrsAtOnceLoadTest_PerfTest() {
        List<String> conversationIdsList = new ArrayList<>();

        Instant requestedAt = null;

        // using classical for loop here due to the use of a mutable requestedAt
        for (Arguments sourceSystemAndEhr : loadTestScenarios().toList()) {
            Gp2GpSystem sourceSystem = (Gp2GpSystem) sourceSystemAndEhr.get()[0];
            LargeEhrVariant ehr = (LargeEhrVariant) sourceSystemAndEhr.get()[1];
            Patient patient = ehr.patient();

            RepoIncomingMessage triggerMessage = new RepoIncomingMessageBuilder()
                    .withPatient(ehr.patient())
                    .withEhrSourceGp(sourceSystem)
                    .withEhrDestinationAsRepo(nhsProperties.getNhsEnvironment())
                    .build();

            log.info("Trigger message: {}", triggerMessage.toJsonString());
            log.info("NHS Number in {} for patient {} is: {}", sourceSystem, patient, patient.nhsNumber());

            setManagingOrganisationToRepo(patient.nhsNumber());

            log.info("Iteration Scenario : Patient {}", patient);
            log.info("Sending to repoIncomingQueue...");
            ehrTransferServiceRepoIncomingQueue.send(triggerMessage);
            requestedAt = Instant.now();

            log.info("Time after sending the triggerMessage to repoIncomingQueue: {}", requestedAt);

            conversationIdsList.add(triggerMessage.getConversationId());
            conversationIdsList.forEach(log::info);
        }

        checkThatTransfersHaveCompletedSuccessfully(conversationIdsList, requestedAt);
    }


    // TODO: ABSTRACT THIS OUT TO ANOTHER CLASS, CAN'T THIS LIVE IN transferTrackerService
    private void checkThatTransfersHaveCompletedSuccessfully(List<String> conversationIdsList, Instant timeLastRequestSent) {
        Instant finishedAt;
        for (String conversationId : conversationIdsList) {
            assertThat(ehrTransferServiceTransferCompleteOQ.getMessageContainingAttribute(
                    "conversationId", conversationId,
                    5, TimeUnit.MINUTES))
                    .isNotNull();

            // get actual transfer time from completion message?
            finishedAt = Instant.now();

            log.info("Time after request sent that completion message found in transferCompleteQueue: {}", finishedAt);

            long timeElapsed = Duration.between(timeLastRequestSent, finishedAt).toSeconds();
            log.info("Total time taken for: " + conversationId + " in seconds was no more than : {}", timeElapsed);

            assertTrue(transferTrackerService.isStatusForConversationIdPresent(conversationId, EHR_TRANSFER_TO_REPO_COMPLETE.status));
        }
    }

    // TODO: ABSTRACT THIS OUT TO ANOTHER CLASS
    private static Stream<Arguments> loadTestScenarios() {
        return Stream.of(
                Arguments.of(EMIS_PTL_INT, LargeEhrVariant.SINGLE_LARGE_FRAGMENT),
                Arguments.of(Gp2GpSystem.TPP_PTL_INT, LargeEhrVariant.SINGLE_LARGE_FRAGMENT),
                Arguments.of(EMIS_PTL_INT, LargeEhrVariant.LARGE_MEDICAL_HISTORY),
                Arguments.of(Gp2GpSystem.TPP_PTL_INT, LargeEhrVariant.LARGE_MEDICAL_HISTORY),
                Arguments.of(EMIS_PTL_INT, LargeEhrVariant.MULTIPLE_LARGE_FRAGMENTS),
                Arguments.of(Gp2GpSystem.TPP_PTL_INT, LargeEhrVariant.MULTIPLE_LARGE_FRAGMENTS)
//
//                // 5mins + variation -> let's run these overnight
//                 Arguments.of(Gp2GpSystem.EMIS_PTL_INT, LargeEhrVariant.HIGH_FRAGMENT_COUNT),
//                 Arguments.of(Gp2GpSystem.TPP_PTL_INT, LargeEhrVariant.HIGH_FRAGMENT_COUNT),
//
//                // 20mins+ -> let's run this overnight
//                 Arguments.of(Gp2GpSystem.EMIS_PTL_INT, LargeEhrVariant.SUPER_LARGE)

                // could not move it to TPP - Large Message general failure
                // Arguments.of(Gp2GpSystem.TPP_PTL_INT, LargeEhrVariant.SUPER_LARGE)
        );
    }

    @ParameterizedTest
    @MethodSource("properWorkingFoundationSupplierSystems")
    void shouldUpdateDbStatusAndPublishToTransferCompleteQueueWhenReceivedNackFromGppSystems(String sourceSystem) {
        final String REQUESTER_NOT_REGISTERED_PRACTICE_FOR_PATIENT_CODE = "19";

        RepoIncomingMessage triggerMessage = new RepoIncomingMessageBuilder()
                .withPatient(Patient.SUSPENDED_WITH_EHR_AT_TPP)
                .withEhrSourceGpOdsCode(sourceSystem)
                .withEhrDestinationAsRepo(nhsProperties.getNhsEnvironment())
                .build();

        ehrTransferServiceRepoIncomingQueue.send(triggerMessage);

        assertThat(ehrTransferServiceNegativeAcknowledgementOQ.getMessageContaining(triggerMessage.getConversationId())).isNotNull();
        assertThat(ehrTransferServiceTransferCompleteOQ.getMessageContainingAttribute("conversationId", triggerMessage.getConversationId())).isNotNull();

        String status = transferTrackerService.waitForStatusMatching(triggerMessage.getConversationId(), TransferTrackerStatus.EHR_TRANSFER_FAILED.status);
        assertThat(status).isEqualTo(TransferTrackerStatus.EHR_TRANSFER_FAILED.status + ":" + REQUESTER_NOT_REGISTERED_PRACTICE_FOR_PATIENT_CODE);
    }

    // TODO: ABSTRACT THIS OUT TO ANOTHER CLASS
    private Stream<Arguments> properWorkingFoundationSupplierSystems() {
        // Exclude EMIS here as our Ptl EMIS instance is not working properly
        return Stream.of(
            Arguments.of(
                gp2GpMessengerProperties.getTppPtlIntOdsCode()
            )
        );
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
        transferTrackerService.save(TransferTrackerRecord.builder()
                        .conversationId(inboundConversationId)
                        .nemsMessageId(randomNemsMessageId())
                        .nhsNumber(nhsNumber)
                        .sourceGp(sendingOdsCode)
                        .state(EHR_REQUEST_SENT.status)
                        .build());

        // Put the patient into mhsInboundQueue as a UK05 message
        mhsInboundQueue.sendMessage(smallEhr, inboundConversationId);

        log.info("conversationIdExists: {}", transferTrackerService.conversationIdExists(inboundConversationId));

        String status = transferTrackerService.waitForStatusMatching(
                inboundConversationId,
                EHR_TRANSFER_TO_REPO_COMPLETE.status);

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

        transferTrackerService.save(TransferTrackerRecord.builder()
                .conversationId(inboundConversationId)
                .largeEhrCoreMessageId(largeEhrCoreMessageId)
                .nemsMessageId(randomNemsMessageId())
                .nhsNumber(nhsNumber)
                .sourceGp(senderOdsCode)
                .state(EHR_REQUEST_SENT.status)
                .build());
        
        // when
        mhsInboundQueue.sendMessage(largeEhrCore, inboundConversationId);

        log.info("conversationIdExists: {}", transferTrackerService.conversationIdExists(inboundConversationId));
        String status = transferTrackerService.waitForStatusMatching(inboundConversationId, LARGE_EHR_CONTINUE_REQUEST_SENT.status);
        log.info("tracker db status: {}", status);
        log.info("fragment 1 message id: {}", fragment1MessageId);
        log.info("fragment 2 message id: {}", fragment2MessageId);

        mhsInboundQueue.sendMessage(largeEhrFragment1, inboundConversationId);
        mhsInboundQueue.sendMessage(largeEhrFragment2, inboundConversationId);

        status = transferTrackerService.waitForStatusMatching(inboundConversationId, EHR_TRANSFER_TO_REPO_COMPLETE.status);
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

    @ParameterizedTest
    @EnumSource(value = TemplateVariant.class, names = {"POSITIVE_ACKNOWLEDGEMENT", "NEGATIVE_ACKNOWLEDGEMENT"})
    void shouldPutAcksOnMHSInboundAndUpdateEhrOutDbStatus(TemplateVariant templateVariant) {
        // given
        UUID ackMessageId = UUID.randomUUID();
        String ackConversationId = UUID.randomUUID().toString();
        String expectedTypeCode = templateVariant.name().equals("POSITIVE_ACKNOWLEDGEMENT") ? "AA" : "AR";

        String ackMessage = this.templatingService.getTemplatedString(templateVariant,
                AcknowledgementTemplateContext.builder()
                        .messageId(ackMessageId)
                        .build());

        // when
        mhsInboundQueue.sendMessage(ackMessage, ackConversationId);

        Acknowledgement acknowledgement = await().atMost(30, TimeUnit.SECONDS)
                .with().pollInterval(2, TimeUnit.SECONDS)
                .until(() -> ehrOutService.findAcknowledgementByMessageId(ackMessageId), Objects::nonNull) ;

        // then
        String actualTypeCode = acknowledgement.getAcknowledgementTypeCode();
        assertThat(actualTypeCode).isEqualTo(expectedTypeCode);
        log.info("The acknowledgement typeCode of {} is {}.", ackMessageId, actualTypeCode);
    }

    // TODO: MOVE THIS METHOD TO THE PDS ADAPTOR SERVICE ITSELF
    private void setManagingOrganisationToRepo(String nhsNumber) {
        PdsAdaptorResponse pdsResponse = pdsAdaptorService.getSuspendedPatientStatus(nhsNumber);
        assertThat(pdsResponse.getIsSuspended()).as("%s should be suspended so that MOF is respected", nhsNumber).isTrue();
        String repoOdsCode = Gp2GpSystem.repoInEnv(nhsProperties.getNhsEnvironment()).odsCode();
        if (!repoOdsCode.equals(pdsResponse.getManagingOrganisation())) {
            pdsAdaptorService.updateManagingOrganisation(nhsNumber, repoOdsCode, pdsResponse.getRecordETag());
        }
    }

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
    @Timeout(value = 60, unit = SECONDS)
    void shouldTransferOut20EHRsWithin1Minute() {
        String nhsNumber = "9727018076";
        String asidCode = "200000000149";
        List<String> outboundConversationIds = Stream.generate(() -> (UUID.randomUUID().toString()))
                .limit(20)
                .toList();

        outboundConversationIds.forEach(conversationId -> {
            EhrRequestTemplateContext ehrRequestTemplateContext = EhrRequestTemplateContext
                    .builder()
                    .outboundConversationId(conversationId.toUpperCase())
                    .nhsNumber(nhsNumber)
                    .sendingOdsCode(TPP_PTL_INT.odsCode())
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
     * Sends the EHR Request to the MHS Inbound queue and waits for 3 seconds.
     * @param message The EHR Request Message.
     * @param outboundConversationId The Outbound Conversation ID.
     */
    private void sendToMhs(String message, String outboundConversationId, MessageType messageType) {
        if(!messageType.toString().equals("EHR_REQUEST") || !messageType.toString().equals("CONTINUE_REQUEST"))
            throw new IllegalArgumentException("Message Type must be EHR_REQUEST or CONTINUE_REQUEST.");

        this.mhsInboundQueue.sendMessage(message, outboundConversationId);

        log.info("A {} was sent to the MHS Inbound Queue, Outbound Conversation ID: {}.",
                messageType.toString(), outboundConversationId);

        switch (messageType) {
            case EHR_REQUEST -> sleepFor(EHR_REQUEST_MHS_INBOUND_TIMEOUT_MILLISECONDS);
            case CONTINUE_REQUEST -> sleepFor(CONTINUE_REQUEST_MHS_INBOUND_TIMEOUT_MILLISECONDS);
        }
    }
  
}
