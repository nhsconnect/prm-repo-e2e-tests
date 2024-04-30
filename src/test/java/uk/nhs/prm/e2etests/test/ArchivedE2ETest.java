package uk.nhs.prm.e2etests.test;

import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import uk.nhs.prm.e2etests.enumeration.*;
import uk.nhs.prm.e2etests.model.RepoIncomingMessage;
import uk.nhs.prm.e2etests.model.RepoIncomingMessageBuilder;
import uk.nhs.prm.e2etests.model.database.Acknowledgement;
import uk.nhs.prm.e2etests.model.response.PdsAdaptorResponse;
import uk.nhs.prm.e2etests.model.templatecontext.*;
import uk.nhs.prm.e2etests.property.Gp2gpMessengerProperties;
import uk.nhs.prm.e2etests.property.NhsProperties;
import uk.nhs.prm.e2etests.queue.SimpleAmqpQueue;
import uk.nhs.prm.e2etests.queue.ehrtransfer.EhrTransferServiceParsingDeadLetterQueue;
import uk.nhs.prm.e2etests.queue.ehrtransfer.EhrTransferServiceRepoIncomingQueue;
import uk.nhs.prm.e2etests.queue.ehrtransfer.observability.*;
import uk.nhs.prm.e2etests.queue.gp2gpmessenger.observability.Gp2GpMessengerOQ;
import uk.nhs.prm.e2etests.service.*;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.nhs.prm.e2etests.enumeration.Gp2GpSystem.EMIS_PTL_INT;
import static uk.nhs.prm.e2etests.utility.TestDataUtility.randomUppercaseUuidAsString;

@Log4j2
@SpringBootTest
@ExtendWith(ForceXercesParserSoLogbackDoesNotBlowUpWhenUsingSwiftMqClient.class)
@TestPropertySource(properties = {"test.pds.username=e2e-test"})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ArchivedE2ETest {
    private final RepoService repoService;
    private final EhrOutService ehrOutService;
    private final PdsAdaptorService pdsAdaptorService;
    private final TemplatingService templatingService;
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
    public ArchivedE2ETest(
            RepoService repoService,
            EhrOutService ehrOutService,
            PdsAdaptorService pdsAdaptorService,
            TemplatingService templatingService,
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
        this.pdsAdaptorService = pdsAdaptorService;
        this.templatingService = templatingService;
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

    // commented out due to connection to oldTransferTrackerService being removed
//    @Disabled("This test was failing before refactoring. The cause seems to be related to EMIS instance not working")
//    @Test
//    void shouldReceivingAndTrackAllLargeEhrFragments_DevAndTest() {
//        Patient largeEhrAtEmisWithRepoMof = Patient.largeEhrAtEmisWithRepoMof(this.nhsProperties.getNhsEnvironment());
//
//        setManagingOrganisationToRepo(largeEhrAtEmisWithRepoMof.nhsNumber());
//
//        RepoIncomingMessage triggerMessage = new RepoIncomingMessageBuilder()
//                .withPatient(largeEhrAtEmisWithRepoMof)
//                .withEhrSourceGp(EMIS_PTL_INT)
//                .withEhrDestinationAsRepo(nhsProperties.getNhsEnvironment())
//                .build();
//
//        ehrTransferServiceRepoIncomingQueue.send(triggerMessage);
//        assertThat(ehrTransferServiceEhrCompleteOQ.getMessageContaining(triggerMessage.getConversationId())).isNotNull();
//        assertTrue(oldTransferTrackerService.isStatusForConversationIdPresent(triggerMessage.getConversationId(), EHR_TRANSFER_TO_REPO_COMPLETE.status));
//    }

    // commented out due to UUID/STRING discrepancy - archived test, not worth fixing at the moment
//    @Disabled
//    @ParameterizedTest
//    @EnumSource(value = TemplateVariant.class, names = {"POSITIVE_ACKNOWLEDGEMENT", "NEGATIVE_ACKNOWLEDGEMENT"})
//    void shouldPutAcksOnMHSInboundAndUpdateEhrOutDbStatus(TemplateVariant templateVariant) {
//        // given
//        String ackMessageId = UUID.randomUUID().toString();
//        String ackConversationId = randomUppercaseUuidAsString();
//        String expectedTypeCode = templateVariant.name().equals("POSITIVE_ACKNOWLEDGEMENT") ? "AA" : "AR";
//
//        log.info("{} conversationId: {}, messageId: {}", templateVariant.name(), ackMessageId, ackConversationId);
//
//        String ackMessage = this.templatingService.getTemplatedString(templateVariant,
//                AcknowledgementTemplateContext.builder()
//                        // If we were to unarchive this, does it need the ackConversationId adding? This might be the fix to the test.
//                        .messageId(ackMessageId)
//                        .build());
//
//        // when
//        mhsInboundQueue.sendMessage(ackMessage, ackConversationId);
//
//        Acknowledgement acknowledgement = await().atMost(30, TimeUnit.SECONDS)
//                .with().pollInterval(2, TimeUnit.SECONDS)
//                .until(() -> ehrOutService.findAcknowledgementByMessageId(ackMessageId), Objects::nonNull) ;
//
//        // then
//        String actualTypeCode = acknowledgement.getAcknowledgementTypeCode();
//        assertThat(actualTypeCode).isEqualTo(expectedTypeCode);
//        log.info("The acknowledgement typeCode of {} is {}.", ackMessageId, actualTypeCode);
//    }

    // commented out due to connection to oldTransferTrackerService being removed
//    @ParameterizedTest
//    @MethodSource("largeEhrScenariosToBeRunAsRequired")
//    @Disabled()
//    void shouldTransferRemainingSizesAndTypesOfEhrs_DevOnly(Gp2GpSystem sourceSystem, LargeEhrVariant largeEhr) {
//        RepoIncomingMessage triggerMessage = new RepoIncomingMessageBuilder()
//                .withPatient(largeEhr.patient())
//                .withEhrSourceGp(sourceSystem)
//                .withEhrDestinationAsRepo(nhsProperties.getNhsEnvironment())
//                .build();
//
//        setManagingOrganisationToRepo(largeEhr.patient().nhsNumber());
//
//        ehrTransferServiceRepoIncomingQueue.send(triggerMessage);
//
//        assertThat(ehrTransferServiceTransferCompleteOQ.getMessageContainingAttribute(
//                "conversationId",
//                triggerMessage.getConversationId(),
//                largeEhr.timeoutMinutes(),
//                TimeUnit.MINUTES))
//                .isNotNull();
//
//        assertTrue(oldTransferTrackerService.isStatusForConversationIdPresent(triggerMessage.getConversationId(), EHR_TRANSFER_TO_REPO_COMPLETE.status));
//    }

    // commented out due to connection to oldTransferTrackerService being removed
//    @ParameterizedTest
//    @Disabled
//    @MethodSource("largeEhrScenariosRunningOnCommit_ButNotEmisWhichIsCurrentlyHavingIssues")
//    void shouldTransferRepresentativeSizesAndTypesOfEhrs_DevOnly(Gp2GpSystem sourceSystem, LargeEhrVariant largeEhr) {
//        RepoIncomingMessage triggerMessage = new RepoIncomingMessageBuilder()
//                .withPatient(largeEhr.patient())
//                .withEhrSourceGp(sourceSystem)
//                .withEhrDestinationAsRepo(nhsProperties.getNhsEnvironment())
//                .build();
//
//        setManagingOrganisationToRepo(largeEhr.patient().nhsNumber());
//
//        ehrTransferServiceRepoIncomingQueue.send(triggerMessage);
//
//        assertThat(ehrTransferServiceTransferCompleteOQ.getMessageContainingAttribute(
//                "conversationId",
//                triggerMessage.getConversationId(),
//                largeEhr.timeoutMinutes(),
//                TimeUnit.MINUTES))
//                .isNotNull();
//
//        assertTrue(oldTransferTrackerService.isStatusForConversationIdPresent(triggerMessage.getConversationId(), EHR_TRANSFER_TO_REPO_COMPLETE.status));
//    }

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

    // TODO: ABSTRACT THIS OUT TO ANOTHER CLASS
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

    // TODO: MOVE THIS METHOD TO THE PDS ADAPTOR SERVICE ITSELF
    private void setManagingOrganisationToRepo(String nhsNumber) {
        PdsAdaptorResponse pdsResponse = pdsAdaptorService.getSuspendedPatientStatus(nhsNumber);
        assertThat(pdsResponse.getIsSuspended()).as("%s should be suspended so that MOF is respected", nhsNumber).isTrue();
        String repoOdsCode = Gp2GpSystem.repoInEnv(nhsProperties.getNhsEnvironment()).odsCode();
        if (!repoOdsCode.equals(pdsResponse.getManagingOrganisation())) {
            pdsAdaptorService.updateManagingOrganisation(nhsNumber, repoOdsCode, pdsResponse.getRecordETag());
        }
    }
}
