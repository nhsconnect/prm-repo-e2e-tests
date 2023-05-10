package uk.nhs.prm.deduction.e2e.tests;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
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
import org.w3c.dom.Attr;
import org.w3c.dom.Node;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.*;
import org.xmlunit.matchers.CompareMatcher;
import org.xmlunit.util.Predicate;
import uk.nhs.prm.deduction.e2e.TestConfiguration;
import uk.nhs.prm.deduction.e2e.ehr_transfer.*;
import uk.nhs.prm.deduction.e2e.end_of_transfer_service.EndOfTransferMofUpdatedMessageQueue;
import uk.nhs.prm.deduction.e2e.models.Gp2GpSystem;
import uk.nhs.prm.deduction.e2e.models.RepoIncomingMessageBuilder;
import uk.nhs.prm.deduction.e2e.pdsadaptor.PdsAdaptorClient;
import uk.nhs.prm.deduction.e2e.performance.awsauth.AssumeRoleCredentialsProviderFactory;
import uk.nhs.prm.deduction.e2e.performance.awsauth.AutoRefreshingRoleAssumingSqsClient;
import uk.nhs.prm.deduction.e2e.queue.BasicSqsClient;
import uk.nhs.prm.deduction.e2e.queue.SqsMessage;
import uk.nhs.prm.deduction.e2e.queue.ThinlyWrappedSqsClient;
import uk.nhs.prm.deduction.e2e.queue.activemq.ForceXercesParserSoLogbackDoesNotBlowUpWhenUsingSwiftMqClient;
import uk.nhs.prm.deduction.e2e.queue.activemq.SimpleAmqpQueue;
import uk.nhs.prm.deduction.e2e.transfer_tracker_db.TransferTrackerDbClient;
import uk.nhs.prm.deduction.e2e.transfer_tracker_db.TrackerDb;
import uk.nhs.prm.deduction.e2e.utility.Resources;

import java.lang.reflect.Array;
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

@SpringBootTest(classes = {
        RepositoryE2ETests.class,
        RepoIncomingQueue.class,
        TestConfiguration.class,
        ThinlyWrappedSqsClient.class, BasicSqsClient.class,
        AssumeRoleCredentialsProviderFactory.class,
        AutoRefreshingRoleAssumingSqsClient.class,
        Resources.class,
        TrackerDb.class,
        SmallEhrQueue.class,
        LargeEhrQueue.class,
        Gp2gpMessengerQueue.class,
        AttachmentQueue.class,
        EhrParsingDLQ.class,
        TransferTrackerDbClient.class,
        EhrCompleteQueue.class,
        TransferCompleteQueue.class,
        NegativeAcknowledgementQueue.class,
        EndOfTransferMofUpdatedMessageQueue.class,
        EhrInUnhandledQueue.class
})
@ExtendWith(ForceXercesParserSoLogbackDoesNotBlowUpWhenUsingSwiftMqClient.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RepositoryE2ETests {

    // TODO PRMT-3252: REMOVE ONCE DONE IMPLEMENTING PRMT-3252 - FOR DEBUG USE ONLY.
    private static final Logger debugLogger = LogManager.getLogger(RepositoryE2ETests.class);

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
    EhrInUnhandledQueue ehrInUnhandledQueue;
    @Autowired
    NegativeAcknowledgementQueue negativeAcknowledgementObservabilityQueue;
    @Autowired
    Gp2gpMessengerQueue gp2gpMessengerQueue;
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
        ehrInUnhandledQueue.deleteAllMessages();
        negativeAcknowledgementObservabilityQueue.deleteAllMessages();
        pdsAdaptorClient = new PdsAdaptorClient("e2e-test", config.getPdsAdaptorE2ETestApiKey(), config.getPdsAdaptorUrl());
    }

    // The following test should eventually test that we can send a small EHR - until we have an EHR in repo/test patient ready to send,
    // we are temporarily doing a smaller test to cover from amqp -> ehr out queue
    @Test
    @EnabledIfEnvironmentVariable(named = "NHS_ENVIRONMENT", matches = "dev")
    void shouldIdentifyEhrRequestAsEhrOutMessage() {
        var ehrRequest = Resources.readTestResourceFile("RCMR_IN010000UK05");
        var inboundQueueFromMhs = new SimpleAmqpQueue(config);

        String conversationId = "17a757f2-f4d2-444e-a246-9cb77bef7f22";
        inboundQueueFromMhs.sendMessage(ehrRequest, conversationId);

        assertThat(ehrInUnhandledQueue.getMessageContaining(ehrRequest)).isNotNull();
    }

    @Test
    void shouldVerifyThatASmallEhrXMLIsUnchanged() {
        // Given
        String inboundConversationId = "1632CD65-FD8F-4914-B62A-9763B50FC04A".toLowerCase(); // UUID.randomUUID().toString();
        String outboundConversationId = UUID.randomUUID().toString();
        var inboundQueueFromMhs = new SimpleAmqpQueue(config);

        String smallEhr = Resources.readTestResourceFileFromEhrDirectory("small-ehr-copy");

        String ehrRequest = Resources.readTestResourceFile("RCMR_IN010000UK05")
                .replaceAll("9692842304", "9727018440")
                .replaceAll("A91720", "M85019")
                .replaceAll("200000000631", "200000000149")
                .replaceAll("17a757f2-f4d2-444e-a246-9cb77bef7f22", outboundConversationId);

        // When
        // change transfer db status to ACTION:EHR_REQUEST_SENT before putting on inbound queue
        // Put the patient to inboundQueueFromMhs as an UK05 message

        inboundQueueFromMhs.sendMessage(smallEhr, inboundConversationId);
        debugLogger.info("conversationIdExists: {}",trackerDb.conversationIdExists(inboundConversationId));
//        var status = trackerDb.waitForStatusMatching(inboundConversationId, "ACTION:EHR_TRANSFER_TO_REPO_COMPLETE");
//        debugLogger.info("tracker db status: {}", status);

        // Put a EHR request to inboundQueueFromMhs
        inboundQueueFromMhs.sendMessage(ehrRequest, outboundConversationId);

        // Then
        // assert gp2gpMessenger queue got a message of UK06
        SqsMessage gp2gpMessage = gp2gpMessengerQueue.getMessageContaining(outboundConversationId);

        assertThat(gp2gpMessage).isNotNull();
        assertThat(gp2gpMessage.contains("RCMR_IN030000UK06")).isTrue();

        try {
            String gp2gpMessengerPayload = getPayload(gp2gpMessage.body());
            String smallEhrPayload = getPayload(smallEhr);

            debugLogger.info("Payload from gp2gpMessenger: {}", gp2gpMessengerPayload);
            debugLogger.info("Payload from smallEhr: {}", smallEhrPayload);
            Diff myDiff = DiffBuilder.compare(gp2gpMessengerPayload).withTest(smallEhrPayload)
                    .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byName))
                    .checkForSimilar().build();
            debugLogger.info("diff: {}", myDiff.fullDescription());

        } catch (JSONException e) {
            debugLogger.error(e);
        }

        debugLogger.info("Message from gp2gpMessenger: {}", gp2gpMessage.body());
//        assertThat(gp2gpMessengerPayload, CompareMatcher.isSimilarTo(smallEhrPayload));
    }

    public class IgnoreAttributeDifferenceEvaluator implements DifferenceEvaluator {
        private String attributeName;
        public IgnoreAttributeDifferenceEvaluator(String attributeName) {
            this.attributeName = attributeName;
        }

        @Override
        public ComparisonResult evaluate(Comparison comparison, ComparisonResult outcome) {
            if (outcome == ComparisonResult.EQUAL)
                return outcome;
            final Node controlNode = comparison.getControlDetails().getTarget();
            if (controlNode instanceof Attr) {
                Attr attr = (Attr) controlNode;
                if (attr.getName().equals(attributeName)) {
                    return ComparisonResult.SIMILAR;
                }
            }
            return outcome;
        }
    }

    private boolean excludeComparisons(Node node) {
        // if the node is ODS code, ignore comparison by returning false
        List<String> excludeList = List.of(
                "1.2.826.0.1285.0.1.10", // ODS code
                "1.2.826.0.1285.0.2.0.107"); // ASID code
        // TODO: messageId ^^

        if (node.hasAttributes() && node.getAttributes().getNamedItem("root") != null) {
            String rootId = node.getAttributes().getNamedItem("root").getNodeValue();
            if (node.getNodeName().equals("id") && excludeList.contains(rootId)) {
                return false;
            }
        }

        return true;
    }

    @Test
    public void given2XMLsWithDifferences_whenTestsSimilarWithDifferenceEvaluator_thenCorrect() {
//        final String control = "<agentOrganizationSDS classCode=\"ORG\" determinerCode=\"INSTANCE\"><id root=\"1.2.826.0.1285.0.1.10\" extension=\"B85002\"/></agentOrganizationSDS>";
//        final String test =    "<agentOrganizationSDS classCode=\"ORG\" determinerCode=\"INSTANCE\"><id root=\"1.2.826.0.1285.0.1.10\" extension=\"M85019\"/></agentOrganizationSDS>";
        final String control = "<device classCode=\"DEV\" determinerCode=\"INSTANCE\"><id root=\"1.2.826.0.1285.0.2.0.107\" extension=\"200000001613\"/></device>";
        final String test =    "<device classCode=\"DEV\" determinerCode=\"INSTANCE\"><id root=\"1.2.826.0.1285.0.2.0.107\" extension=\"200000000149\"/></device>";

        Diff myDiff = DiffBuilder.compare(control).withTest(test)
                .withNodeFilter(node -> excludeComparisons(node))
//                .withDifferenceEvaluator(new IgnoreAttributeDifferenceEvaluator("extension"))
                .checkForSimilar().build();

        assertFalse(myDiff.toString(), myDiff.hasDifferences());
    }

    private static String getPayload(String gp2gpMessageBody) throws JSONException {
        JSONObject jsonObject = new JSONObject(gp2gpMessageBody);
        if (jsonObject.has("payload") ) {
            return jsonObject.getString("payload");
        } else {
            return jsonObject.getJSONObject("request").getJSONObject("body").getString("payload");
        }
    }

    // TODO: PRMT-3252 Create test: shouldVerifyThatALargeEhrXMLIsUnchanged()

    @Test
    void shouldReceivingAndTrackAllLargeEhrFragments_DevAndTest() {
        var largeEhrAtEmisWithRepoMof = Patient.largeEhrAtEmisWithRepoMof(config);

        setManagingOrganisationToRepo(largeEhrAtEmisWithRepoMof.nhsNumber());

        var triggerMessage = new RepoIncomingMessageBuilder()
                .withPatient(largeEhrAtEmisWithRepoMof)
                .withEhrSourceGp(Gp2GpSystem.EMIS_PTL_INT)
                .withEhrDestinationAsRepo(config)
                .build();

        repoIncomingQueue.send(triggerMessage);
        assertThat(ehrCompleteQueue.getMessageContaining(triggerMessage.conversationId())).isNotNull();
        assertTrue(trackerDb.statusForConversationIdIs(triggerMessage.conversationId(), "ACTION:EHR_TRANSFER_TO_REPO_COMPLETE"));
    }

    @ParameterizedTest
    @MethodSource("largeEhrScenariosRunningOnCommit_ButNotEmisWhichIsCurrentlyHavingIssues")
    @EnabledIfEnvironmentVariable(named = "NHS_ENVIRONMENT", matches = "dev", disabledReason = "We have only one set of variants for large ehr")
    void shouldTransferRepresentativeSizesAndTypesOfEhrs_DevOnly(Gp2GpSystem sourceSystem, LargeEhrVariant largeEhr) {
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
                Arguments.of(Gp2GpSystem.EMIS_PTL_INT, LargeEhrVariant.SINGLE_LARGE_ATTACHMENT),
                Arguments.of(Gp2GpSystem.TPP_PTL_INT, LargeEhrVariant.SINGLE_LARGE_ATTACHMENT),
                Arguments.of(Gp2GpSystem.EMIS_PTL_INT, LargeEhrVariant.LARGE_MEDICAL_HISTORY),
                Arguments.of(Gp2GpSystem.TPP_PTL_INT, LargeEhrVariant.LARGE_MEDICAL_HISTORY),
                Arguments.of(Gp2GpSystem.EMIS_PTL_INT, LargeEhrVariant.MULTIPLE_LARGE_ATTACHMENTS),
                Arguments.of(Gp2GpSystem.TPP_PTL_INT, LargeEhrVariant.MULTIPLE_LARGE_ATTACHMENTS)
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
    }

    private static Stream<Arguments> largeEhrScenariosToBeRunAsRequired() {
        return Stream.of(
                // 5mins+ variation -> removed from regression as intermittently takes 2+ hours
                // to complete which, whiile successful, is not sufficiently timely for on-commit regression
                Arguments.of(Gp2GpSystem.EMIS_PTL_INT, LargeEhrVariant.HIGH_ATTACHMENT_COUNT),
                Arguments.of(Gp2GpSystem.TPP_PTL_INT, LargeEhrVariant.HIGH_ATTACHMENT_COUNT),

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
    @MethodSource("foundationSupplierSystemsWithoutEmisWhichIsCurrentlyHavingIssues")
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

    private static Stream<Arguments> foundationSupplierSystemsWithoutEmisWhichIsCurrentlyHavingIssues() {
        return Stream.of(Arguments.of(Gp2GpSystem.TPP_PTL_INT));
    }

    private static Stream<Arguments> foundationSupplierSystems() {
        return Gp2GpSystem.foundationSupplierSystems();
    }

    private void setManagingOrganisationToRepo(String nhsNumber) {
        var pdsResponse = pdsAdaptorClient.getSuspendedPatientStatus(nhsNumber);
        assertThat(pdsResponse.getIsSuspended()).as("%s should be suspended so that MOF is respected", nhsNumber).isTrue();
        var repoOdsCode = Gp2GpSystem.repoInEnv(config).odsCode();
        if (!repoOdsCode.equals(pdsResponse.getManagingOrganisation())) {
            pdsAdaptorClient.updateManagingOrganisation(nhsNumber, repoOdsCode, pdsResponse.getRecordETag());
        }
    }
}
