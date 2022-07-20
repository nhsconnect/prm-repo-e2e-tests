package uk.nhs.prm.deduction.e2e.tests;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
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
import uk.nhs.prm.deduction.e2e.queue.ActiveMqClient;
import uk.nhs.prm.deduction.e2e.queue.BasicSqsClient;
import uk.nhs.prm.deduction.e2e.queue.SqsQueue;
import uk.nhs.prm.deduction.e2e.transfer_tracker_db.DbClient;
import uk.nhs.prm.deduction.e2e.transfer_tracker_db.TrackerDb;
import uk.nhs.prm.deduction.e2e.utility.Resources;

import javax.jms.JMSException;
import java.util.UUID;
import java.util.stream.Stream;

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
        ActiveMqClient.class,
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
    ActiveMqClient mqClient;

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
    EndOfTransferMofUpdatedMessageQueue endOfTransferMofUpdatedQueue;
    @Autowired
    TestConfiguration config;

    PdsAdaptorClient pdsAdaptorClient;

    @BeforeAll
    void init() {
        smallEhrQueue.deleteAllMessages();
        largeEhrQueue.deleteAllMessages();
        attachmentQueue.deleteAllMessages();
        parsingDLQ.deleteAllMessages();
        negativeAcknowledgementObservabilityQueue.deleteAllMessages();
        pdsAdaptorClient = new PdsAdaptorClient("e2e-test", config.getPdsAdaptorE2ETestApiKey(), config.getPdsAdaptorUrl());
    }

    @Test
    void shouldTestThatMessagesAreReadCorrectlyFromRepoIncomingQueueAndAnEhrRequestIsMadeAndTheDbIsUpdatedWithExpectedStatus() {  //this test would expand and change as progress
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
    void shouldReadMessageFromInboundActiveMQProcessAndPutItOnSmallEhrAndEhrCompleteQueues() throws JMSException {  //this test would expand and change as progress
        var conversationId = generateConversationId();
        mqClient.postAMessageToAQueue("inbound", getMessageWithUniqueConversationIdAndMessageId("unsanitized_small_ehr", conversationId));
        assertThat(smallEhrQueue.getMessageContaining(conversationId));
        assertThat(ehrCompleteQueue.getMessageContaining(conversationId));
    }

    private String generateConversationId() {
        var conversationId = UUID.randomUUID().toString();
        System.out.println("conversation Id " + conversationId);
        System.out.flush();
        return conversationId;
    }

    @Test
    void shouldPutLargeEhrFromInboundActiveMQAndObserveItOnLargeEhrObservabilityQueue() throws JMSException {  //this test would expand and change as progress
        var conversationId = generateConversationId();
        mqClient.postAMessageToAQueue("inbound", getMessageWithUniqueConversationIdAndMessageId("unsanitized_large_ehr", conversationId));
        assertThat(largeEhrQueue.getMessageContainingAttribute("conversationId", conversationId));
    }

    @Test
    void shouldPutMessageWithAttachmentsFromInboundActiveMQAndObserveItOnAttachmentsObservabilityQueue() throws JMSException {  //this test would expand and change as progress
        String conversationId = generateConversationId();
        mqClient.postAMessageToAQueue("inbound", getMessageWithUniqueConversationIdAndMessageId("message_with_attachment", conversationId));
        assertThat(attachmentQueue.getMessageContaining(conversationId));
    }

    @Test
    void shouldPutAUnprocessableMessageFromInboundActiveMqToDLQ() throws JMSException {  //this test would expand and change as progress
        String dlqMessage = "A DLQ MESSAGE";
        System.out.println("dlq message " + dlqMessage);
        mqClient.postAMessageToAQueue("inbound", dlqMessage);
        assertThat(parsingDLQ.getMessageContaining(dlqMessage));
    }

    @Test
    void shouldTestTheE2EJourneyForALargeEhrReceivingAllTheFragmentsAndUpdatingTheDBWithHealthRecordStatus() {  //this test would expand and change as progress
        var largeEhrAtEmisWithRepoMof = Patient.largeEhrAtEmisWithRepoMof(config);
        setOdsCodeToRepo(largeEhrAtEmisWithRepoMof.nhsNumber());
        var triggerMessage = new RepoIncomingMessageBuilder()
                .withPatient(largeEhrAtEmisWithRepoMof)
                .withEhrSourceGp(Gp2GpSystem.EMIS_PTL_INT)
                .withEhrDestinationAsRepo(config)
                .build();

        repoIncomingQueue.send(triggerMessage);
        assertThat(ehrCompleteQueue.getMessageContaining(triggerMessage.conversationId()));
        assertTrue(trackerDb.statusForConversationIdIs(triggerMessage.conversationId(), "ACTION:EHR_TRANSFER_TO_REPO_COMPLETE"));
//        assertThat(endOfTransferMofUpdatedQueue.getMessageContaining(triggerMessage.getNemsMessageIdAsString())); TODO change dev patient to dev synthetic patient
    }

    @Disabled("small-large-ehr-core-messages not working see PRMT-2712 :/")
    @ParameterizedTest
    @MethodSource("varietyOfLargeEhrs")
    void shouldTransferAllSizesAndTypesOfEhrs(LargeEhrVariant largeEhr) {
        var triggerMessage = new RepoIncomingMessageBuilder()
                .withPatient(largeEhr.patient())
                .withEhrSourceGp(Gp2GpSystem.EMIS_PTL_INT) // and TPP
                .withEhrDestinationAsRepo(config)
                .build();

        repoIncomingQueue.send(triggerMessage);

        assertThat(transferCompleteQueue.getMessageContainingAttribute("conversationId", triggerMessage.conversationId()));
    }

    private static Stream<Arguments> varietyOfLargeEhrs() {
        return Stream.of(Arguments.of(LargeEhrVariant.SINGLE_ATTACHMENT));
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

    private void setOdsCodeToRepo(String nhsNumber) {
        var pdsResponse = pdsAdaptorClient.getSuspendedPatientStatus(nhsNumber);
        var repoOdsCode = Gp2GpSystem.repoInEnv(config).odsCode();
        if (!repoOdsCode.equals(pdsResponse.getManagingOrganisation())) {
            pdsAdaptorClient.updateManagingOrganisation(nhsNumber, repoOdsCode, pdsResponse.getRecordETag());
        }
    }

    private static Stream<Arguments> foundationSupplierSystems() {
        return Gp2GpSystem.foundationSupplierSystems();
    }

    private String getMessageWithUniqueConversationIdAndMessageId(String fileName, String conversationId) {
        String messageId = UUID.randomUUID().toString();
        String attachment1MessageId = UUID.randomUUID().toString();
        String attachment2MessageId = UUID.randomUUID().toString();
        String message = Resources.readTestResourceFileFromEhrDirectory(fileName);
        message = message.replaceAll("__conversationId__", conversationId);
        message = message.replaceAll("__messageId__", messageId);
        message = message.replaceAll("__Attachment1_messageId__", attachment1MessageId);
        message = message.replace("__Attachment2_messageId__", attachment2MessageId);
        return message;
    }
}
