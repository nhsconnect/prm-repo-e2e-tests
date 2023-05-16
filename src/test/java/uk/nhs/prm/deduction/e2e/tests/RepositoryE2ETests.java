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
import org.w3c.dom.Node;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.*;
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
import uk.nhs.prm.deduction.e2e.transfer_tracker_db.TransferTrackerDbMessage;
import uk.nhs.prm.deduction.e2e.utility.Resources;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.util.AssertionErrors.assertFalse;
import static uk.nhs.prm.deduction.e2e.nhs.NhsIdentityGenerator.randomNemsMessageId;
import static uk.nhs.prm.deduction.e2e.utility.TestUtils.isValidUUID;

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
    private static final Logger LOGGER = LogManager.getLogger(RepositoryE2ETests.class);

    private final RepoIncomingQueue repoIncomingQueue;
    private final TrackerDb trackerDb;
    private final SmallEhrQueue smallEhrQueue;
    private final LargeEhrQueue largeEhrQueue;
    private final AttachmentQueue attachmentQueue;
    private final EhrParsingDLQ parsingDLQ;
    private final EhrCompleteQueue ehrCompleteQueue;
    private final TransferCompleteQueue transferCompleteQueue;
    private final EhrInUnhandledQueue ehrInUnhandledQueue;
    private final NegativeAcknowledgementQueue negativeAcknowledgementObservabilityQueue;
    private final Gp2gpMessengerQueue gp2gpMessengerQueue;
    private final TestConfiguration config;

    @Autowired
    public RepositoryE2ETests(
            RepoIncomingQueue repoIncomingQueue,
            TrackerDb trackerDb,
            SmallEhrQueue smallEhrQueue,
            LargeEhrQueue largeEhrQueue,
            AttachmentQueue attachmentQueue,
            EhrParsingDLQ parsingDLQ,
            EhrCompleteQueue ehrCompleteQueue,
            TransferCompleteQueue transferCompleteQueue,
            EhrInUnhandledQueue ehrInUnhandledQueue,
            NegativeAcknowledgementQueue negativeAcknowledgementObservabilityQueue,
            Gp2gpMessengerQueue gp2gpMessengerQueue,
            TestConfiguration config

    ) {
        this.repoIncomingQueue = repoIncomingQueue;
        this.trackerDb = trackerDb;
        this.smallEhrQueue = smallEhrQueue;
        this.largeEhrQueue = largeEhrQueue;
        this.attachmentQueue = attachmentQueue;
        this.parsingDLQ = parsingDLQ;
        this.ehrCompleteQueue = ehrCompleteQueue;
        this.transferCompleteQueue = transferCompleteQueue;
        this.ehrInUnhandledQueue = ehrInUnhandledQueue;
        this.negativeAcknowledgementObservabilityQueue = negativeAcknowledgementObservabilityQueue;
        this.gp2gpMessengerQueue = gp2gpMessengerQueue;
        this.config = config;
    }

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
        String inboundConversationId = UUID.randomUUID().toString();
        String smallEhrMessageId = UUID.randomUUID().toString();
        String outboundConversationId = UUID.randomUUID().toString();
        String nhsNumberForTestPatient = "9727018440";
        String sourceGpForTestPatient = "M85019";
        String timeNow = ZonedDateTime.now(ZoneOffset.ofHours(0)).toString();

        var inboundQueueFromMhs = new SimpleAmqpQueue(config);

        String smallEhr = Resources.readTestResourceFileFromEhrDirectory("small-ehr-without-linebreaks")
                .replaceAll("1632CD65-FD8F-4914-B62A-9763B50FC04A", inboundConversationId.toUpperCase())
                .replaceAll("0206C270-E9A0-11ED-808B-AC162D1F16F0", smallEhrMessageId);

        String ehrRequest = Resources.readTestResourceFile("RCMR_IN010000UK05")
                .replaceAll("9692842304", nhsNumberForTestPatient)
                .replaceAll("A91720", sourceGpForTestPatient)
                .replaceAll("200000000631", "200000000149")
                .replaceAll("17a757f2-f4d2-444e-a246-9cb77bef7f22", outboundConversationId);

        // When
        // change transfer db status to ACTION:EHR_REQUEST_SENT before putting on inbound queue
        // Put the patient to inboundQueueFromMhs as an UK05 message

        trackerDb.save(new TransferTrackerDbMessage(
                inboundConversationId,
                "",
                randomNemsMessageId(),
                nhsNumberForTestPatient,
                sourceGpForTestPatient,
                "ACTION:EHR_REQUEST_SENT",
                timeNow,
                timeNow,
                timeNow
        ));

        inboundQueueFromMhs.sendMessage(smallEhr, inboundConversationId);
        LOGGER.info("conversationIdExists: {}",trackerDb.conversationIdExists(inboundConversationId));
        var status = trackerDb.waitForStatusMatching(inboundConversationId, "ACTION:EHR_TRANSFER_TO_REPO_COMPLETE");
        LOGGER.info("tracker db status: {}", status);

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

            LOGGER.info("Payload from gp2gpMessenger: {}", gp2gpMessengerPayload);
            LOGGER.info("Payload from smallEhr: {}", smallEhrPayload);
            Diff myDiff = DiffBuilder.compare(gp2gpMessengerPayload).withTest(smallEhrPayload)
                    .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byName))
                    .withNodeFilter(this::excludeComparisons)
                    .withDifferenceEvaluator(DifferenceEvaluators.chain(DifferenceEvaluators.Default,
                            DifferenceEvaluators.downgradeDifferencesToEqual(ComparisonType.XML_STANDALONE)))
                    .checkForSimilar().build();

            assertFalse(myDiff.toString(), myDiff.hasDifferences());

        } catch (JSONException e) {
            LOGGER.error(e);
            throw new Error(e);
        }
    }

    private boolean excludeComparisons(Node node) {
        List<String> excludeList = List.of(
                "1.2.826.0.1285.0.1.10", // ODS code
                "1.2.826.0.1285.0.2.0.107" // ASID code
        );

        if (node.hasAttributes() && node.getAttributes().getNamedItem("root") != null) {
            String idRootValue = node.getAttributes().getNamedItem("root").getNodeValue();
            // return false to skip comparison in case when id root value itself is a message id
            if (isValidUUID(idRootValue)) {
                return false;
            }
            // return false to skip comparison when the type of compared value is in the excludedList
            return !(node.getNodeName().equals("id") && excludeList.contains(idRootValue));
        }
        return true;
    }

    @Test
    public void given2XMLsWithDifferences_whenTestsSimilarWithDifferenceEvaluator_thenCorrect() {
        // helper test case for building the `excludeComparisons` function
//        final String control = "<agentOrganizationSDS classCode=\"ORG\" determinerCode=\"INSTANCE\"><id root=\"1.2.826.0.1285.0.1.10\" extension=\"B85002\"/></agentOrganizationSDS>";
//        final String test =    "<agentOrganizationSDS classCode=\"ORG\" determinerCode=\"INSTANCE\"><id root=\"1.2.826.0.1285.0.1.10\" extension=\"M85019\"/></agentOrganizationSDS>";
//        final String control = "<device classCode=\"DEV\" determinerCode=\"INSTANCE\"><id root=\"1.2.826.0.1285.0.2.0.107\" extension=\"200000001613\"/></device>";
//        final String test =    "<device classCode=\"DEV\" determinerCode=\"INSTANCE\"><id root=\"1.2.826.0.1285.0.2.0.107\" extension=\"200000000149\"/></device>";
//        final String control = "<id root=\"DF91D420-DDC7-11ED-808B-AC162D1F16F0\"/>";
//        final String test    = "<id root=\"DFBA6AC0-DDC7-11ED-808B-AC162D1F16F0\"/>";
        final String control = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\" ?><RCMR_IN030000UK06 xmlns=\"urn:hl7-org:v3\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"urn:hl7-org:v3 ..\\Schemas\\RCMR_IN030000UK06.xsd\"></RCMR_IN030000UK06>";
        final String test    = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?><RCMR_IN030000UK06 xmlns=\"urn:hl7-org:v3\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"urn:hl7-org:v3 ..\\Schemas\\RCMR_IN030000UK06.xsd\"></RCMR_IN030000UK06>";


        Diff myDiff = DiffBuilder.compare(control).withTest(test)
                .withNodeFilter(this::excludeComparisons)
                .withDifferenceEvaluator(DifferenceEvaluators.chain(DifferenceEvaluators.Default,
                        DifferenceEvaluators.downgradeDifferencesToEqual(ComparisonType.XML_STANDALONE)))
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

    @Test
    void shouldVerifyThatALargeEhrXMLIsUnchanged() {
        // given
        String inboundConversationId = UUID.randomUUID().toString();
        String largeEhrCoreMessageId = UUID.randomUUID().toString();
        String fragment1MessageId = UUID.randomUUID().toString();
        String fragment2MessageId = UUID.randomUUID().toString();

        String outboundConversationId = UUID.randomUUID().toString();
        String nhsNumberForTestPatient = "9727018157";
        String previousGpForTestPatient = "N82668";
        String newGpForTestPatient = "M85019";
        String timeNow = ZonedDateTime.now(ZoneOffset.ofHours(0)).toString();

        var inboundQueueFromMhs = new SimpleAmqpQueue(config);

        String largeEhrCore = Resources.readTestResourceFileFromEhrDirectory("large-ehr-core")
                .replaceAll("71118F7D-59CE-4552-B7AE-45A6801F4334", inboundConversationId.toUpperCase())
                .replaceAll("B8DC074D-C039-4FD2-8BBB-D4BFBBBF9AFA", largeEhrCoreMessageId)
                .replaceAll("3DBFC9EB-32FA-444F-B996-AB680D64148E", fragment1MessageId);

        String largeEhrFragment1 = Resources.readTestResourceFileFromEhrDirectory("large-ehr-fragment-1")
                .replaceAll("71118F7D-59CE-4552-B7AE-45A6801F4334", inboundConversationId.toUpperCase())
                .replaceAll("3DBFC9EB-32FA-444F-B996-AB680D64148E", fragment1MessageId)
                .replaceAll("03CBFB18-0F7E-4BB6-B9EF-46AF564D3B9C", fragment2MessageId);

        String largeEhrFragment2 = Resources.readTestResourceFileFromEhrDirectory("large-ehr-fragment-2")
                .replaceAll("71118F7D-59CE-4552-B7AE-45A6801F4334", inboundConversationId.toUpperCase())
                .replaceAll("03CBFB18-0F7E-4BB6-B9EF-46AF564D3B9C", fragment2MessageId);

        String ehrRequest = Resources.readTestResourceFile("RCMR_IN010000UK05")
                .replaceAll("9692842304", nhsNumberForTestPatient)
                .replaceAll("A91720", newGpForTestPatient)
                .replaceAll("17a757f2-f4d2-444e-a246-9cb77bef7f22", outboundConversationId);

        String continueRequest = Resources.readTestResourceFile("COPC_IN000001UK01")
                .replaceAll("A91720", newGpForTestPatient)
                .replaceAll("a86e85ef-7ba9-46dd-ab48-da804c8bede6", outboundConversationId);


        trackerDb.save(new TransferTrackerDbMessage(
                inboundConversationId,
                largeEhrCoreMessageId,
                randomNemsMessageId(),
                nhsNumberForTestPatient,
                previousGpForTestPatient,
                "ACTION:EHR_REQUEST_SENT",
                timeNow,
                timeNow,
                timeNow
        ));

        // when
        inboundQueueFromMhs.sendMessage(largeEhrCore, inboundConversationId);
        LOGGER.info("conversationIdExists: {}",trackerDb.conversationIdExists(inboundConversationId));
        var status = trackerDb.waitForStatusMatching(inboundConversationId, "ACTION:LARGE_EHR_CONTINUE_REQUEST_SENT");
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

        try {
            String gp2gpMessengerPayload = getPayload(gp2gpMessageUK06.body());
            String largeEhrCorePayload = getPayload(largeEhrCore);

            LOGGER.info("Payload from gp2gpMessenger: {}", gp2gpMessengerPayload);
            LOGGER.info("Payload from largeEhrCore: {}", largeEhrCorePayload);
            Diff myDiff = DiffBuilder.compare(gp2gpMessengerPayload).withTest(largeEhrCorePayload)
                    .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byName))
                    .withNodeFilter(this::excludeComparisons)
                    .withDifferenceEvaluator(DifferenceEvaluators.chain(DifferenceEvaluators.Default,
                            DifferenceEvaluators.downgradeDifferencesToEqual(ComparisonType.XML_STANDALONE)))
                    .checkForSimilar().build();

            assertFalse(myDiff.toString(), myDiff.hasDifferences());

        } catch (JSONException e) {
            LOGGER.error(e);
            throw new Error(e);
        }

        inboundQueueFromMhs.sendMessage(continueRequest, outboundConversationId);

        // assert gp2gpMessenger queue got COPC ehr fragment
//        SqsMessage gp2gpMessageCOPC = gp2gpMessengerQueue.getMessageContaining("COPC_IN000001UK01");
//
//        assertThat(gp2gpMessageCOPC).isNotNull();
//        assertThat(gp2gpMessageCOPC.contains(outboundConversationId)).isTrue();
    }

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

            assertTrue(trackerDb.statusForConversationIdIs(conversationId, "ACTION:EHR_TRANSFER_TO_REPO_COMPLETE"));
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
