package uk.nhs.prm.deduction.e2e.tests;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.nhs.prm.deduction.e2e.TestConfiguration;
import uk.nhs.prm.deduction.e2e.ehr_transfer.*;
import uk.nhs.prm.deduction.e2e.end_of_transfer_service.EndOfTransferMofUpdatedMessageQueue;
import uk.nhs.prm.deduction.e2e.models.Gp2GpSystem;
import uk.nhs.prm.deduction.e2e.models.RepoIncomingMessageBuilder;
import uk.nhs.prm.deduction.e2e.pdsadaptor.PdsAdaptorClient;
import uk.nhs.prm.deduction.e2e.performance.awsauth.AssumeRoleCredentialsProviderFactory;
import uk.nhs.prm.deduction.e2e.performance.awsauth.AutoRefreshingRoleAssumingSqsClient;
import uk.nhs.prm.deduction.e2e.queue.BasicSqsClient;
import uk.nhs.prm.deduction.e2e.queue.SqsQueue;
import uk.nhs.prm.deduction.e2e.transfer_tracker_db.DbClient;
import uk.nhs.prm.deduction.e2e.transfer_tracker_db.TrackerDb;
import uk.nhs.prm.deduction.e2e.utility.Resources;

import javax.jms.JMSException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = {
        RepositoryE2ETests.class,
        RepoIncomingQueue.class,
        TestConfiguration.class,
        SqsQueue.class, BasicSqsClient.class,
        AssumeRoleCredentialsProviderFactory.class,
        AutoRefreshingRoleAssumingSqsClient.class,
        Resources.class,
        TrackerDb.class,
        SmallEhrQueue.class,
        LargeEhrQueue.class,
        AttachmentQueue.class,
        EhrParsingDLQ.class,
        DbClient.class,
        EhrCompleteQueue.class,
        TransferCompleteQueue.class,
        NegativeAcknowledgementQueue.class,
        EndOfTransferMofUpdatedMessageQueue.class
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RepositoryE2ETests {

    @Autowired
    RepoIncomingQueue repoIncomingQueue;

    @Autowired
    TrackerDb trackerDb;
    @Autowired
    SmallEhrQueue smallEhrQueue;
    @Autowired
    LargeEhrQueue largeEhrQueue;
    @Autowired
    AttachmentQueue attachmentQueue;
    @Autowired
    EhrParsingDLQ parsingDLQ;
    @Autowired
    EhrCompleteQueue ehrCompleteQueue;
    @Autowired
    TransferCompleteQueue transferCompleteQueue;
    @Autowired
    NegativeAcknowledgementQueue negativeAcknowledgementObservabilityQueue;
    @Autowired
    TestConfiguration config;

    PdsAdaptorClient pdsAdaptorClient;

    @BeforeAll
    void init() {
        smallEhrQueue.deleteAllMessages();
        largeEhrQueue.deleteAllMessages();
        attachmentQueue.deleteAllMessages();
        parsingDLQ.deleteAllMessages();
        transferCompleteQueue.deleteAllMessages();
        negativeAcknowledgementObservabilityQueue.deleteAllMessages();
        pdsAdaptorClient = new PdsAdaptorClient("e2e-test", config.getPdsAdaptorE2ETestApiKey(), config.getPdsAdaptorUrl());
    }

    @Test
    void shouldTestThatMessagesAreReadCorrectlyFromRepoIncomingQueueAndAnEhrRequestIsMadeAndTheDbIsUpdatedWithExpectedStatus() {
        var triggerMessage = new RepoIncomingMessageBuilder()
                .withPatient(Patient.WITH_NO_9693795989_WHATEVER_THAT_MEANS)
                .withEhrSourceGp(Gp2GpSystem.EMIS_PTL_INT)
                .withEhrDestinationGp(Gp2GpSystem.repoInEnv(config))
                .build();

        repoIncomingQueue.send(triggerMessage);

        assertTrue(trackerDb.conversationIdExists(triggerMessage.conversationId()));
        assertTrue(trackerDb.statusForConversationIdIs(triggerMessage.conversationId(), "ACTION:EHR_REQUEST_SENT"));
    }

    @Test
    void shouldTestTheE2EJourneyForALargeEhrReceivingAllTheFragmentsAndUpdatingTheDBWithHealthRecordStatus_DevAndTest() {
        var largeEhrAtEmisWithRepoMof = Patient.largeEhrAtEmisWithRepoMof(config);

        setManagingOrganisationToRepo(largeEhrAtEmisWithRepoMof.nhsNumber());

        var triggerMessage = new RepoIncomingMessageBuilder()
                .withPatient(largeEhrAtEmisWithRepoMof)
                .withEhrSourceGp(Gp2GpSystem.EMIS_PTL_INT)
                .withEhrDestinationAsRepo(config)
                .build();

        repoIncomingQueue.send(triggerMessage);
        assertThat(ehrCompleteQueue.getMessageContaining(triggerMessage.conversationId()));
        assertTrue(trackerDb.statusForConversationIdIs(triggerMessage.conversationId(), "ACTION:EHR_TRANSFER_TO_REPO_COMPLETE"));
//        assertThat(endOfTransferMofUpdatedQueue.getMessageContaining(triggerMessage.getNemsMessageIdAsString())); TODO change dev patient to dev synthetic patient - what's this Jack?
    }

    @ParameterizedTest
    @MethodSource("largeEhrScenarios")
    @EnabledIfEnvironmentVariable(named = "NHS_ENVIRONMENT", matches = "dev", disabledReason = "We have only one set of variants for large ehr")
    void shouldTransferAllSizesAndTypesOfEhrs_DevOnly(Gp2GpSystem sourceSystem, LargeEhrVariant largeEhr) {
        var triggerMessage = new RepoIncomingMessageBuilder()
                .withPatient(largeEhr.patient())
                .withEhrSourceGp(sourceSystem)
                .withEhrDestinationAsRepo(config)
                .build();

        setManagingOrganisationToRepo(largeEhr.patient().nhsNumber());

        repoIncomingQueue.send(triggerMessage);

        assertThat(transferCompleteQueue.getMessageContainingAttribute(
                "conversationId",
                triggerMessage.conversationId(),
                largeEhr.timeoutMinutes(),
                TimeUnit.MINUTES));

        assertTrue(trackerDb.statusForConversationIdIs(triggerMessage.conversationId(), "ACTION:EHR_TRANSFER_TO_REPO_COMPLETE"));

        // assert in ehr-repo? need to check all messages complete?
    }

    private static Stream<Arguments> largeEhrScenarios() {
        return Stream.of(
                Arguments.of(Gp2GpSystem.EMIS_PTL_INT, LargeEhrVariant.SINGLE_LARGE_ATTACHMENT),
                Arguments.of(Gp2GpSystem.TPP_PTL_INT, LargeEhrVariant.SINGLE_LARGE_ATTACHMENT),
                Arguments.of(Gp2GpSystem.EMIS_PTL_INT, LargeEhrVariant.LARGE_MEDICAL_HISTORY),
                Arguments.of(Gp2GpSystem.TPP_PTL_INT, LargeEhrVariant.LARGE_MEDICAL_HISTORY),
                Arguments.of(Gp2GpSystem.EMIS_PTL_INT, LargeEhrVariant.MULTIPLE_LARGE_ATTACHMENTS),
                Arguments.of(Gp2GpSystem.TPP_PTL_INT, LargeEhrVariant.MULTIPLE_LARGE_ATTACHMENTS)

                // 5mins + variation -> let's run these overnight
                // Arguments.of(Gp2GpSystem.EMIS_PTL_INT, LargeEhrVariant.HIGH_ATTACHMENT_COUNT),
                // Arguments.of(Gp2GpSystem.TPP_PTL_INT, LargeEhrVariant.HIGH_ATTACHMENT_COUNT),

                // 20mins+ -> let's run this overnight
                // Arguments.of(Gp2GpSystem.EMIS_PTL_INT, LargeEhrVariant.SUPER_LARGE)

                // could not move it to TPP - Large Message general failure
                // Arguments.of(Gp2GpSystem.TPP_PTL_INT, LargeEhrVariant.SUPER_LARGE)
        );
    }

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
                    .withEhrDestinationAsRepo(config)
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

            assertTrue(trackerDb.statusForConversationIdIs(conversationId.toString(), "ACTION:EHR_TRANSFER_TO_REPO_COMPLETE"));
        }
    }

    private static Stream<Arguments> loadTestScenarios() {
        return Stream.of(
                Arguments.of(Gp2GpSystem.EMIS_PTL_INT, LargeEhrVariant.SINGLE_LARGE_ATTACHMENT),
                Arguments.of(Gp2GpSystem.TPP_PTL_INT, LargeEhrVariant.SINGLE_LARGE_ATTACHMENT),
                Arguments.of(Gp2GpSystem.EMIS_PTL_INT, LargeEhrVariant.LARGE_MEDICAL_HISTORY),
                Arguments.of(Gp2GpSystem.TPP_PTL_INT, LargeEhrVariant.LARGE_MEDICAL_HISTORY),
                Arguments.of(Gp2GpSystem.EMIS_PTL_INT, LargeEhrVariant.MULTIPLE_LARGE_ATTACHMENTS),
                Arguments.of(Gp2GpSystem.TPP_PTL_INT, LargeEhrVariant.MULTIPLE_LARGE_ATTACHMENTS)
//
//                // 5mins + variation -> let's run these overnight
//                 Arguments.of(Gp2GpSystem.EMIS_PTL_INT, LargeEhrVariant.HIGH_ATTACHMENT_COUNT),
//                 Arguments.of(Gp2GpSystem.TPP_PTL_INT, LargeEhrVariant.HIGH_ATTACHMENT_COUNT),
//
//                // 20mins+ -> let's run this overnight
//                 Arguments.of(Gp2GpSystem.EMIS_PTL_INT, LargeEhrVariant.SUPER_LARGE)

                // could not move it to TPP - Large Message general failure
                // Arguments.of(Gp2GpSystem.TPP_PTL_INT, LargeEhrVariant.SUPER_LARGE)
        );
    }

    @ParameterizedTest
    @MethodSource("foundationSupplierSystems")
    void shouldUpdateDbStatusAndPublishToTransferCompleteQueueWhenReceivedNackFromGppSystems(Gp2GpSystem sourceSystem) {
        final var REQUESTER_NOT_REGISTERED_PRACTICE_FOR_PATIENT_CODE = "19";

        var triggerMessage = new RepoIncomingMessageBuilder()
                .withPatient(Patient.SUSPENDED_WITH_EHR_AT_TPP)
                .withEhrSourceGp(sourceSystem)
                .withEhrDestinationAsRepo(config)
                .build();

        repoIncomingQueue.send(triggerMessage);

        assertThat(negativeAcknowledgementObservabilityQueue.getMessageContaining(triggerMessage.conversationId()));
        assertThat(transferCompleteQueue.getMessageContainingAttribute("conversationId", triggerMessage.conversationId()));

        var status = trackerDb.waitForStatusMatching(triggerMessage.conversationId(), "ACTION:EHR_TRANSFER_FAILED");
        assertThat(status).isEqualTo("ACTION:EHR_TRANSFER_FAILED:" + REQUESTER_NOT_REGISTERED_PRACTICE_FOR_PATIENT_CODE);
    }

    private static Stream<Arguments> foundationSupplierSystems() {
        return Gp2GpSystem.foundationSupplierSystems();
    }

    private void setManagingOrganisationToRepo(String nhsNumber) {
        var pdsResponse = pdsAdaptorClient.getSuspendedPatientStatus(nhsNumber);
        var repoOdsCode = Gp2GpSystem.repoInEnv(config).odsCode();
        if (!repoOdsCode.equals(pdsResponse.getManagingOrganisation())) {
            pdsAdaptorClient.updateManagingOrganisation(nhsNumber, repoOdsCode, pdsResponse.getRecordETag());
        }
    }
}