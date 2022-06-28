package uk.nhs.prm.deduction.e2e.tests;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.nhs.prm.deduction.e2e.TestConfiguration;
import uk.nhs.prm.deduction.e2e.ehr_transfer.*;
import uk.nhs.prm.deduction.e2e.models.Gp2GpSystem;
import uk.nhs.prm.deduction.e2e.models.RepoIncomingMessageBuilder;
import uk.nhs.prm.deduction.e2e.performance.awsauth.AssumeRoleCredentialsProviderFactory;
import uk.nhs.prm.deduction.e2e.performance.awsauth.AutoRefreshingRoleAssumingSqsClient;
import uk.nhs.prm.deduction.e2e.queue.ActiveMqClient;
import uk.nhs.prm.deduction.e2e.queue.BasicSqsClient;
import uk.nhs.prm.deduction.e2e.queue.SqsQueue;
import uk.nhs.prm.deduction.e2e.transfer_tracker_db.DbClient;
import uk.nhs.prm.deduction.e2e.transfer_tracker_db.TrackerDb;
import uk.nhs.prm.deduction.e2e.utility.Resources;

import javax.jms.JMSException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;

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
        NegativeAcknowledgementQueue.class
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

    @BeforeAll
    void init() {
        smallEhrQueue.deleteAllMessages();
        largeEhrQueue.deleteAllMessages();
        attachmentQueue.deleteAllMessages();
        parsingDLQ.deleteAllMessages();
        negativeAcknowledgementObservabilityQueue.deleteAllMessages();
    }

    @Test
    void shouldTestThatMessagesAreReadCorrectlyFromRepoIncomingQueueAndAnEhrRequestIsMadeAndTheDbIsUpdatedWithExpectedStatus() {  //this test would expand and change as progress
        var triggerMessage = new RepoIncomingMessageBuilder()
                .withPatient(Patient.WITH_NO_9693795989_WHATEVER_THAT_MEANS)
                .withEhrSourceGp(Gp2GpSystem.EMIS_PTL_INT)
                .withEhrDestinationGp(Gp2GpSystem.REPO_DEV)
                .build();

        repoIncomingQueue.send(triggerMessage);

        assertTrue(trackerDb.conversationIdExists(triggerMessage.conversationId()));
        assertTrue(trackerDb.statusForConversationIdIs(triggerMessage.conversationId(), "ACTION:EHR_REQUEST_SENT"));
    }


    @Test
    void shouldReadMessageFromInboundActiveMQProcessAndPutItOnSmallEhrAndEhrCompleteQueues() throws JMSException {  //this test would expand and change as progress
        String conversationId = UUID.randomUUID().toString();
        System.out.println("conversation Id " + conversationId);
        mqClient.postAMessageToAQueue("inbound", getMessageWithUniqueConversationIdAndMessageId("unsanitized_small_ehr", conversationId));
        assertThat(smallEhrQueue.getMessageContaining(conversationId));
        assertThat(ehrCompleteQueue.getMessageContaining(conversationId));
    }

    @Test
    void shouldPutLargeEhrFromInboundActiveMQAndObserveItOnLargeEhrObservabilityQueue() throws JMSException {  //this test would expand and change as progress
        String conversationId = UUID.randomUUID().toString();
        System.out.println("conversation Id " + conversationId);
        mqClient.postAMessageToAQueue("inbound", getMessageWithUniqueConversationIdAndMessageId("unsanitized_large_ehr", conversationId));
        assertThat(largeEhrQueue.getMessageContainingAttribute("conversationId", conversationId));
    }

    @Test
    void shouldPutMessageWithAttachmentsFromInboundActiveMQAndObserveItOnAttachmentsObservabilityQueue() throws JMSException {  //this test would expand and change as progress
        String conversationId = UUID.randomUUID().toString();
        System.out.println("conversation Id " + conversationId);
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
        var triggerMessage = new RepoIncomingMessageBuilder()
                .withPatient(Patient.WITH_NO_9693643038_WHATEVER_THAT_MEANS)
                .withEhrSourceGp(Gp2GpSystem.EMIS_PTL_INT)
                .withEhrDestinationGp(Gp2GpSystem.REPO_DEV)
                .build();

        repoIncomingQueue.send(triggerMessage);
        assertThat(ehrCompleteQueue.getMessageContaining(triggerMessage.conversationId()));
        assertTrue(trackerDb.statusForConversationIdIs(triggerMessage.conversationId(), "ACTION:EHR_TRANSFER_TO_REPO_COMPLETE"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "TPP_PTL_INT", "EMIS_PTL_INT" })
    void shouldUpdateDbStatusAndPublishToTransferCompleteQueueWhenReceivedNackFromGppSystems(String sourceSystem) {
        final var REQUESTER_NOT_REGISTERED_PRACTICE_FOR_PATIENT_CODE = "19";

        var message = new RepoIncomingMessageBuilder()
                .withPatient(Patient.SUSPENDED_WITH_EHR_AT_TPP)
                .withEhrSourceGp(Gp2GpSystem.valueOf(sourceSystem))
                .withEhrDestinationGp(Gp2GpSystem.REPO_DEV)
                .build();

        repoIncomingQueue.send(message);

        assertThat(negativeAcknowledgementObservabilityQueue.getMessageContaining(message.conversationId()));
        assertThat(transferCompleteQueue.getMessageContainingAttribute("conversationId", message.conversationId()));

        var status = trackerDb.waitForStatusMatching(message.conversationId(), "ACTION:EHR_TRANSFER_FAILED");
        assertThat(status).isEqualTo("ACTION:EHR_TRANSFER_FAILED:" + REQUESTER_NOT_REGISTERED_PRACTICE_FOR_PATIENT_CODE);
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
