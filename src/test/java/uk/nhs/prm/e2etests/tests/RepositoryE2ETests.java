package uk.nhs.prm.e2etests.tests;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.xmlunit.diff.*;
import uk.nhs.prm.e2etests.TestConfiguration;
import uk.nhs.prm.e2etests.configuration.NhsPropertySource;
import uk.nhs.prm.e2etests.configuration.PdsAdaptorPropertySource;
import uk.nhs.prm.e2etests.configuration.QueuePropertySource;
import uk.nhs.prm.e2etests.ehr_transfer.*;
import uk.nhs.prm.e2etests.model.Gp2GpSystem;
import uk.nhs.prm.e2etests.model.RepoIncomingMessage;
import uk.nhs.prm.e2etests.model.RepoIncomingMessageBuilder;
import uk.nhs.prm.e2etests.pdsadaptor.PdsAdaptorClient;
import uk.nhs.prm.e2etests.enumeration.LargeEhrVariant;
import uk.nhs.prm.e2etests.enumeration.Patient;
import uk.nhs.prm.e2etests.queue.*;
import uk.nhs.prm.e2etests.transfer_tracker_db.TrackerDb;
import uk.nhs.prm.e2etests.configuration.Gp2gpMessengerPropertySource;
import uk.nhs.prm.e2etests.utility.LargeEhrTestFiles;
import uk.nhs.prm.e2etests.utility.Resources;
import uk.nhs.prm.e2etests.utility.TestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.util.AssertionErrors.assertFalse;
import static uk.nhs.prm.e2etests.utility.TestUtils.*;

@SpringBootTest
@ExtendWith(ForceXercesParserSoLogbackDoesNotBlowUpWhenUsingSwiftMqClient.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RepositoryE2ETests {
    private static final Logger LOGGER = LogManager.getLogger(RepositoryE2ETests.class);

    private final RepoIncomingQueue repoIncomingQueue;
    private final TrackerDb trackerDb;
    private final SmallEhrQueue smallEhrQueue;
    private final LargeEhrQueue largeEhrQueue;
    private final LargeEhrFragmentsQueue fragmentQueue;
    private final EhrParsingDLQ parsingDLQ;
    private final EhrCompleteQueue ehrCompleteQueue;
    private final TransferCompleteQueue transferCompleteQueue;
    private final EhrInUnhandledQueue ehrInUnhandledQueue;
    private final NegativeAcknowledgementQueue negativeAcknowledgementObservabilityQueue;
    private final Gp2gpMessengerQueue gp2gpMessengerQueue;
    private final TestConfiguration testConfiguration;
    private final Gp2gpMessengerPropertySource gp2GpMessengerPropertySource;
    private final QueuePropertySource queuePropertySource;
    private final PdsAdaptorPropertySource pdsAdaptorPropertySource;
    private final NhsPropertySource nhsPropertySource;

    @Autowired
    public RepositoryE2ETests(
            RepoIncomingQueue repoIncomingQueue,
            TrackerDb trackerDb,
            SmallEhrQueue smallEhrQueue,
            LargeEhrQueue largeEhrQueue,
            LargeEhrFragmentsQueue fragmentQueue,
            EhrParsingDLQ parsingDLQ,
            EhrCompleteQueue ehrCompleteQueue,
            TransferCompleteQueue transferCompleteQueue,
            EhrInUnhandledQueue ehrInUnhandledQueue,
            NegativeAcknowledgementQueue negativeAcknowledgementObservabilityQueue,
            Gp2gpMessengerQueue gp2gpMessengerQueue,
            TestConfiguration testConfiguration,
            Gp2gpMessengerPropertySource gp2GpMessengerPropertySource,
            QueuePropertySource queuePropertySource,
            PdsAdaptorPropertySource pdsAdaptorPropertySource,
            NhsPropertySource nhsPropertySource
    ) {
        this.repoIncomingQueue = repoIncomingQueue;
        this.trackerDb = trackerDb;
        this.smallEhrQueue = smallEhrQueue;
        this.largeEhrQueue = largeEhrQueue;
        this.fragmentQueue = fragmentQueue;
        this.parsingDLQ = parsingDLQ;
        this.ehrCompleteQueue = ehrCompleteQueue;
        this.transferCompleteQueue = transferCompleteQueue;
        this.ehrInUnhandledQueue = ehrInUnhandledQueue;
        this.negativeAcknowledgementObservabilityQueue = negativeAcknowledgementObservabilityQueue;
        this.gp2gpMessengerQueue = gp2gpMessengerQueue;
        this.testConfiguration = testConfiguration;
        this.gp2GpMessengerPropertySource = gp2GpMessengerPropertySource;
        this.queuePropertySource = queuePropertySource;
        this.pdsAdaptorPropertySource = pdsAdaptorPropertySource;
        this.nhsPropertySource = nhsPropertySource;
    }

    PdsAdaptorClient pdsAdaptorClient;

    @BeforeAll
    void init() {
        smallEhrQueue.deleteAllMessages();
        largeEhrQueue.deleteAllMessages();
        fragmentQueue.deleteAllMessages();
        parsingDLQ.deleteAllMessages();
        transferCompleteQueue.deleteAllMessages();
        ehrInUnhandledQueue.deleteAllMessages();
        negativeAcknowledgementObservabilityQueue.deleteAllMessages();
        gp2gpMessengerQueue.deleteAllMessages();
        pdsAdaptorClient = new PdsAdaptorClient(
                "e2e-test",
                pdsAdaptorPropertySource.getE2eTestApiKey(),
                pdsAdaptorPropertySource.getPdsAdaptorUrl()
        );
    }

    // The following test should eventually test that we can send a small EHR - until we have an EHR in repo/test patient ready to send,
    // we are temporarily doing a smaller test to cover from amqp -> ehr out queue
    @Test
    @EnabledIfEnvironmentVariable(named = "NHS_ENVIRONMENT", matches = "dev")
    void shouldIdentifyEhrRequestAsEhrOutMessage() {
        var ehrRequest = Resources.readTestResourceFile("ehr/ehr-request");
        var inboundQueueFromMhs = new SimpleAmqpQueue(queuePropertySource, testConfiguration);

        String conversationId = "17a757f2-f4d2-444e-a246-9cb77bef7f22";
        inboundQueueFromMhs.sendMessage(ehrRequest, conversationId);

        assertThat(ehrInUnhandledQueue.getMessageContaining(ehrRequest)).isNotNull();
    }

    @Test
    void shouldVerifyThatASmallEhrXMLIsUnchanged() {
        // Given
        String inboundConversationId = UUID.randomUUID().toString();
        String smallEhrMessageId = UUID.randomUUID().toString();
        String outboundConversationId = UUID.randomUUID().toString();
        String nhsNumberForTestPatient = "9727018440";
        String previousGpForTestPatient = "M85019";
        String asidCodeForTestPatient = "200000000149";

        SimpleAmqpQueue inboundQueueFromMhs = new SimpleAmqpQueue(queuePropertySource, testConfiguration);

        String smallEhr = getSmallEhrWithoutLinebreaks(inboundConversationId.toUpperCase(), smallEhrMessageId);
        String ehrRequest = getEhrRequest(nhsNumberForTestPatient, previousGpForTestPatient, asidCodeForTestPatient, outboundConversationId);

        // When
        // change transfer db status to ACTION:EHR_REQUEST_SENT before putting on inbound queue
        // Put the patient into inboundQueueFromMhs as a UK05 message

        addRecordToTrackerDb(trackerDb, inboundConversationId, "", nhsNumberForTestPatient, previousGpForTestPatient, "ACTION:EHR_REQUEST_SENT");
        inboundQueueFromMhs.sendMessage(smallEhr, inboundConversationId);

        LOGGER.info("conversationIdExists: {}",trackerDb.conversationIdExists(inboundConversationId));
        String status = trackerDb.waitForStatusMatching(inboundConversationId, "ACTION:EHR_TRANSFER_TO_REPO_COMPLETE");
        LOGGER.info("tracker db status: {}", status);

        // Put a EHR request to inboundQueueFromMhs
        inboundQueueFromMhs.sendMessage(ehrRequest, outboundConversationId);

        // Then
        // assert gp2gpMessenger queue got a message of UK06
        SqsMessage gp2gpMessage = gp2gpMessengerQueue.getMessageContaining(outboundConversationId);

        assertThat(gp2gpMessage).isNotNull();
        assertThat(gp2gpMessage.contains("RCMR_IN030000UK06")).isTrue();

        String gp2gpMessengerPayload = getPayloadOptional(gp2gpMessage.body()).orElseThrow();
        String smallEhrPayload = getPayloadOptional(smallEhr).orElseThrow();
        LOGGER.info("Payload from gp2gpMessenger: {}", gp2gpMessengerPayload);
        LOGGER.info("Payload from smallEhr: {}", smallEhrPayload);

        Diff myDiff = comparePayloads(gp2gpMessengerPayload, smallEhrPayload);

        assertFalse(myDiff.toString(), myDiff.hasDifferences());
    }

    // TODO: THIS TEST WAS FAILING BEFORE REFACTOR
    @Test
    void shouldVerifyThatALargeEhrXMLIsUnchanged() {
        // given
        String inboundConversationId = UUID.randomUUID().toString();
        String outboundConversationId = UUID.randomUUID().toString();
        String largeEhrCoreMessageId = UUID.randomUUID().toString();
        String fragment1MessageId = UUID.randomUUID().toString();
        String fragment2MessageId = UUID.randomUUID().toString();

        String nhsNumberForTestPatient = "9727018157";
        String previousGpForTestPatient = "N82668";
        String newGpForTestPatient = "M85019";

        SimpleAmqpQueue inboundQueueFromMhs = new SimpleAmqpQueue(queuePropertySource, testConfiguration);

        LargeEhrTestFiles largeEhrTestFiles = TestUtils.prepareTestFilesForLargeEhr(
                inboundConversationId,
                outboundConversationId,
                largeEhrCoreMessageId,
                fragment1MessageId,
                fragment2MessageId,
                newGpForTestPatient,
                nhsNumberForTestPatient
        );

        String largeEhrCore = largeEhrTestFiles.largeEhrCore;
        String largeEhrFragment1 = largeEhrTestFiles.largeEhrFragment1;
        String largeEhrFragment2 = largeEhrTestFiles.largeEhrFragment2;
        String ehrRequest = largeEhrTestFiles.ehrRequest;
        String continueRequest = largeEhrTestFiles.continueRequest;

        addRecordToTrackerDb(trackerDb, inboundConversationId, largeEhrCoreMessageId, nhsNumberForTestPatient, previousGpForTestPatient, "ACTION:EHR_REQUEST_SENT");

        // when
        inboundQueueFromMhs.sendMessage(largeEhrCore, inboundConversationId);
        LOGGER.info("conversationIdExists: {}",trackerDb.conversationIdExists(inboundConversationId));
        String status = trackerDb.waitForStatusMatching(inboundConversationId, "ACTION:LARGE_EHR_CONTINUE_REQUEST_SENT");
        LOGGER.info("tracker db status: {}", status);

        LOGGER.info("fragment 1 message id: {}", fragment1MessageId);
        LOGGER.info("fragment 2 message id: {}", fragment2MessageId);

        inboundQueueFromMhs.sendMessage(largeEhrFragment1, inboundConversationId);
        inboundQueueFromMhs.sendMessage(largeEhrFragment2, inboundConversationId);

        status = trackerDb.waitForStatusMatching(inboundConversationId, "ACTION:EHR_TRANSFER_TO_REPO_COMPLETE");
        LOGGER.info("tracker db status: {}", status);

        // Put a EHR request to inboundQueueFromMhs
        inboundQueueFromMhs.sendMessage(ehrRequest, outboundConversationId);

        // Then
        // assert gp2gpMessenger queue got a message of UK06
        SqsMessage gp2gpMessageUK06 = gp2gpMessengerQueue.getMessageContaining(outboundConversationId);

        assertThat(gp2gpMessageUK06).isNotNull();
        assertThat(gp2gpMessageUK06.contains("RCMR_IN030000UK06")).isTrue();

        String gp2gpMessengerEhrCorePayload = getPayloadOptional(gp2gpMessageUK06.body()).orElseThrow();
        String largeEhrCorePayload = getPayloadOptional(largeEhrCore).orElseThrow();

        Diff compareEhrCores = comparePayloads(gp2gpMessengerEhrCorePayload, largeEhrCorePayload);
        boolean ehrCoreIsIdentical = !compareEhrCores.hasDifferences();
        assertTrue(ehrCoreIsIdentical);

        // Put a continue request to inboundQueueFromMhs
        inboundQueueFromMhs.sendMessage(continueRequest, outboundConversationId);

        // get all message fragments from gp2gp-messenger observability queue and compare with inbound fragments
        List<SqsMessage> allFragments = gp2gpMessengerQueue.getAllMessageContaining("ehr/continue-request");
        assertThat(allFragments.size()).isGreaterThanOrEqualTo(2);

        String largeEhrFragment1Payload = getPayloadOptional(largeEhrFragment1).orElseThrow();
        String largeEhrFragment2Payload = getPayloadOptional(largeEhrFragment2).orElseThrow();

        allFragments.forEach(fragment -> {
            assertThat(fragment.contains(outboundConversationId)).isTrue();

            String fragmentPayload = getPayloadOptional(fragment.body()).orElseThrow();
            Diff compareWithFragment1 = comparePayloads(fragmentPayload, largeEhrFragment1Payload);
            Diff compareWithFragment2 = comparePayloads(fragmentPayload, largeEhrFragment2Payload);

            boolean identicalWithFragment1 = !compareWithFragment1.hasDifferences();
            boolean identicalWithFragment2 = !compareWithFragment2.hasDifferences();

            assertTrue(identicalWithFragment1 || identicalWithFragment2);
        });
    }

    // THIS TEST WAS FAILING BEFORE REFACTOR
    @Test
    void shouldReceivingAndTrackAllLargeEhrFragments_DevAndTest() {
        var largeEhrAtEmisWithRepoMof = Patient.largeEhrAtEmisWithRepoMof(testConfiguration);

        setManagingOrganisationToRepo(largeEhrAtEmisWithRepoMof.nhsNumber());

        var triggerMessage = new RepoIncomingMessageBuilder()
                .withPatient(largeEhrAtEmisWithRepoMof)
                .withEhrSourceGp(Gp2GpSystem.EMIS_PTL_INT)
                .withEhrDestinationAsRepo(nhsPropertySource.getNhsEnvironment())
                .build();

        repoIncomingQueue.send(triggerMessage);
        assertThat(ehrCompleteQueue.getMessageContaining(triggerMessage.conversationId())).isNotNull();
        assertTrue(trackerDb.statusForConversationIdIs(triggerMessage.conversationId(), "ACTION:EHR_TRANSFER_TO_REPO_COMPLETE"));
    }

    @ParameterizedTest
    @MethodSource("largeEhrScenariosRunningOnCommit_ButNotEmisWhichIsCurrentlyHavingIssues")
    @EnabledIfEnvironmentVariable(named = "NHS_ENVIRONMENT", matches = "dev", disabledReason = "We have only one set of variants for large ehr")
    void shouldTransferRepresentativeSizesAndTypesOfEhrs_DevOnly(Gp2GpSystem sourceSystem, LargeEhrVariant largeEhr) {
        RepoIncomingMessage triggerMessage = new RepoIncomingMessageBuilder()
                .withPatient(largeEhr.patient())
                .withEhrSourceGp(sourceSystem)
                .withEhrDestinationAsRepo(nhsPropertySource.getNhsEnvironment())
                .build();

        setManagingOrganisationToRepo(largeEhr.patient().nhsNumber());

        repoIncomingQueue.send(triggerMessage);

        assertThat(transferCompleteQueue.getMessageContainingAttribute(
                "conversationId",
                triggerMessage.conversationId(),
                largeEhr.timeoutMinutes(),
                TimeUnit.MINUTES));

        assertTrue(trackerDb.statusForConversationIdIs(triggerMessage.conversationId(), "ACTION:EHR_TRANSFER_TO_REPO_COMPLETE"));

        // option: assert in ehr-repo - check all messages complete - evaluate need based on:
        //  - ehr out round trip testing
        //  - implementation of PRMT-2972
    }

    private static Stream<Arguments> largeEhrScenariosRunningOnCommit_ButNotEmisWhichIsCurrentlyHavingIssues() {
        return largeEhrScenariosRunningOnCommit().filter(args ->
                args.get()[0] != Gp2GpSystem.EMIS_PTL_INT);
    }

    private static Stream<Arguments> largeEhrScenariosRunningOnCommit() {
        return Stream.of(
                Arguments.of(Gp2GpSystem.EMIS_PTL_INT, LargeEhrVariant.SINGLE_LARGE_FRAGMENT),
                Arguments.of(Gp2GpSystem.TPP_PTL_INT, LargeEhrVariant.SINGLE_LARGE_FRAGMENT),
                Arguments.of(Gp2GpSystem.EMIS_PTL_INT, LargeEhrVariant.LARGE_MEDICAL_HISTORY),
                Arguments.of(Gp2GpSystem.TPP_PTL_INT, LargeEhrVariant.LARGE_MEDICAL_HISTORY),
                Arguments.of(Gp2GpSystem.EMIS_PTL_INT, LargeEhrVariant.MULTIPLE_LARGE_FRAGMENTS),
                Arguments.of(Gp2GpSystem.TPP_PTL_INT, LargeEhrVariant.MULTIPLE_LARGE_FRAGMENTS)
        );
    }

    @ParameterizedTest
    @MethodSource("largeEhrScenariosToBeRunAsRequired")
    @EnabledIfEnvironmentVariable(named = "NHS_ENVIRONMENT", matches = "dev", disabledReason = "We have only one set of variants for large ehr")
    @EnabledIfEnvironmentVariable(named = "RUN_ALL_VARIANTS", matches = "true", disabledReason = "Too slow / problematic for on-commit run")
    void shouldTransferRemainingSizesAndTypesOfEhrs_DevOnly(Gp2GpSystem sourceSystem, LargeEhrVariant largeEhr) {
        var triggerMessage = new RepoIncomingMessageBuilder()
                .withPatient(largeEhr.patient())
                .withEhrSourceGp(sourceSystem)
                .withEhrDestinationAsRepo(nhsPropertySource.getNhsEnvironment())
                .build();

        setManagingOrganisationToRepo(largeEhr.patient().nhsNumber());

        repoIncomingQueue.send(triggerMessage);

        assertThat(transferCompleteQueue.getMessageContainingAttribute(
                "conversationId",
                triggerMessage.conversationId(),
                largeEhr.timeoutMinutes(),
                TimeUnit.MINUTES));

        assertTrue(trackerDb.statusForConversationIdIs(triggerMessage.conversationId(), "ACTION:EHR_TRANSFER_TO_REPO_COMPLETE"));
    }

    private static Stream<Arguments> largeEhrScenariosToBeRunAsRequired() {
        return Stream.of(
                // 5mins+ variation -> removed from regression as intermittently takes 2+ hours
                // to complete which, whiile successful, is not sufficiently timely for on-commit regression
                Arguments.of(Gp2GpSystem.EMIS_PTL_INT, LargeEhrVariant.HIGH_FRAGMENT_COUNT),
                Arguments.of(Gp2GpSystem.TPP_PTL_INT, LargeEhrVariant.HIGH_FRAGMENT_COUNT),

                // 20mins+, filling FSS disks causing outages -> to be run ad hoc as needed
                 Arguments.of(Gp2GpSystem.EMIS_PTL_INT, LargeEhrVariant.SUPER_LARGE)

                // could not move it EMIS to TPP - Large Message general failure
                // need to establish current TPP limits that are applying in this case
                // Arguments.of(Gp2GpSystem.TPP_PTL_INT, LargeEhrVariant.SUPER_LARGE)
        );
    }

    @Disabled("Running manually in dev environment.")
    @Test
    void shouldHandleMultipleEhrsAtOnceLoadTest_PerfTest() {
        var conversationIdsList = new ArrayList<String>();

        Instant requestedAt = null;
        var iterationIndex = 1;
        for (Arguments sourceSystemAndEhr : loadTestScenarios().collect(toList())) {
            var sourceSystem = (Gp2GpSystem) sourceSystemAndEhr.get()[0];
            var ehr = (LargeEhrVariant) sourceSystemAndEhr.get()[1];
            var patient = ehr.patient();

            var triggerMessage = new RepoIncomingMessageBuilder()
                    .withPatient(ehr.patient())
                    .withEhrSourceGp(sourceSystem)
                    .withEhrDestinationAsRepo(nhsPropertySource.getNhsEnvironment())
                    .build();

            System.out.println("Trigger message: " + triggerMessage.toJsonString());
            //System.out.println("NHS Number in " + sourceSystem + " for patient " + patient + " is: " + patient.nhsNumber());

            setManagingOrganisationToRepo(patient.nhsNumber());

            System.out.println("Iteration Scenario : " + iterationIndex + " : Patient " + patient);
            System.out.println("Sending to repoIncomingQueue...");
            repoIncomingQueue.send(triggerMessage);
            requestedAt = Instant.now();

            System.out.println("Time after sending the triggerMessage to repoIncomingQueue: " + requestedAt);

            conversationIdsList.add(triggerMessage.getConversationIdAsString());
            conversationIdsList.forEach(System.out::println);

            iterationIndex++;

        }

        checkThatTransfersHaveCompletedSuccessfully(conversationIdsList, requestedAt);
    }

    private void checkThatTransfersHaveCompletedSuccessfully(ArrayList<String> conversationIdsList, Instant timeLastRequestSent) {
        Instant finishedAt;
        for (var conversationId : conversationIdsList) {
            assertThat(transferCompleteQueue.getMessageContainingAttribute("conversationId", conversationId, 5, TimeUnit.MINUTES));

            // get actual transfer time from completion message?
            finishedAt = Instant.now();

            System.out.println("Time after request sent that completion message found in transferCompleteQueue: " + finishedAt);

            long timeElapsed = Duration.between(timeLastRequestSent, finishedAt).toSeconds();
            System.out.println("Total time taken for: " + conversationId + " in seconds was no more than : " + timeElapsed);

            assertTrue(trackerDb.statusForConversationIdIs(conversationId, "ACTION:EHR_TRANSFER_TO_REPO_COMPLETE"));
        }
    }

    private static Stream<Arguments> loadTestScenarios() {
        return Stream.of(
                Arguments.of(Gp2GpSystem.EMIS_PTL_INT, LargeEhrVariant.SINGLE_LARGE_FRAGMENT),
                Arguments.of(Gp2GpSystem.TPP_PTL_INT, LargeEhrVariant.SINGLE_LARGE_FRAGMENT),
                Arguments.of(Gp2GpSystem.EMIS_PTL_INT, LargeEhrVariant.LARGE_MEDICAL_HISTORY),
                Arguments.of(Gp2GpSystem.TPP_PTL_INT, LargeEhrVariant.LARGE_MEDICAL_HISTORY),
                Arguments.of(Gp2GpSystem.EMIS_PTL_INT, LargeEhrVariant.MULTIPLE_LARGE_FRAGMENTS),
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
    @MethodSource("foundationSupplierSystemsWithoutEmisWhichIsCurrentlyHavingIssues")
    void shouldUpdateDbStatusAndPublishToTransferCompleteQueueWhenReceivedNackFromGppSystems(String sourceSystem) {
        final var REQUESTER_NOT_REGISTERED_PRACTICE_FOR_PATIENT_CODE = "19";

        var triggerMessage = new RepoIncomingMessageBuilder()
                .withPatient(Patient.SUSPENDED_WITH_EHR_AT_TPP)
                .withEhrSourceGpOdsCode(sourceSystem)
                .withEhrDestinationAsRepo(nhsPropertySource.getNhsEnvironment())
                .build();

        repoIncomingQueue.send(triggerMessage);

        assertThat(negativeAcknowledgementObservabilityQueue.getMessageContaining(triggerMessage.conversationId()));
        assertThat(transferCompleteQueue.getMessageContainingAttribute("conversationId", triggerMessage.conversationId()));


        var status = trackerDb.waitForStatusMatching(triggerMessage.conversationId(), "ACTION:EHR_TRANSFER_FAILED");
        assertThat(status).isEqualTo("ACTION:EHR_TRANSFER_FAILED:" + REQUESTER_NOT_REGISTERED_PRACTICE_FOR_PATIENT_CODE);
    }

    // TODO: PRMT-3488 Rename; "CurrentlyHavingIssues" implies method name is temporary
    private Stream<Arguments> foundationSupplierSystemsWithoutEmisWhichIsCurrentlyHavingIssues() {
        return Stream.of(
            Arguments.of(
                gp2GpMessengerPropertySource.getEmisPtlIntOdsCode()
            )
        );
    }

    private void setManagingOrganisationToRepo(String nhsNumber) {
        var pdsResponse = pdsAdaptorClient.getSuspendedPatientStatus(nhsNumber);
        assertThat(pdsResponse.getIsSuspended()).as("%s should be suspended so that MOF is respected", nhsNumber).isTrue();
        var repoOdsCode = Gp2GpSystem.repoInEnv(nhsPropertySource.getNhsEnvironment()).odsCode();
        if (!repoOdsCode.equals(pdsResponse.getManagingOrganisation())) {
            pdsAdaptorClient.updateManagingOrganisation(nhsNumber, repoOdsCode, pdsResponse.getRecordETag());
        }
    }
}
