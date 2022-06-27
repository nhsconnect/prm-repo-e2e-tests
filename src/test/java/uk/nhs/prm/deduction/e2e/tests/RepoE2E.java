package uk.nhs.prm.deduction.e2e.tests;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.nhs.prm.deduction.e2e.TestConfiguration;
import uk.nhs.prm.deduction.e2e.ehr_transfer.*;
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
        RepoE2E.class,
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
        NegativeAcknowledgementQueue.class
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RepoE2E {

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
        String nhsNumber = "9693795989";
        String nemsMessageId = UUID.randomUUID().toString();
        String conversationId = UUID.randomUUID().toString();

        String message = "{\"nhsNumber\":\"" + nhsNumber + "\",\"nemsMessageId\":\"" + nemsMessageId + "\",\"nemsEventLastUpdated\":\"" + ZonedDateTime.now(ZoneOffset.ofHours(0)) + "\", \"sourceGp\":\"N82668\",\"destinationGp\":\"B85002\",\"conversationId\":\"" + conversationId + "\"}";
        repoIncomingQueue.postAMessage(message);
        assertTrue(trackerDb.conversationIdExists(conversationId));
        assertTrue(trackerDb.statusForConversationIdIs(conversationId, "ACTION:EHR_REQUEST_SENT"));
    }


    @Test
    void shouldReadMessageFromInboundActiveMQProcessAndPutItOnSmallEhrAndEhrCompleteQueues() throws JMSException {  //this test would expand and change as progress
        String conversationId = UUID.randomUUID().toString();
        System.out.println("conversation Id " + conversationId);
        mqClient.postAMessageToAQueue("inbound", GetMessageWithUniqueConversationIdAndMessageId("unsanitized_small_ehr", conversationId));
        assertThat(smallEhrQueue.getMessageContaining(conversationId));
        assertThat(ehrCompleteQueue.getMessageContaining(conversationId));
    }

    @Test
    void shouldPutLargeEhrFromInboundActiveMQAndObserveItOnLargeEhrObservabilityQueue() throws JMSException {  //this test would expand and change as progress
        String conversationId = UUID.randomUUID().toString();
        System.out.println("conversation Id " + conversationId);
        mqClient.postAMessageToAQueue("inbound", GetMessageWithUniqueConversationIdAndMessageId("unsanitized_large_ehr", conversationId));
        assertThat(largeEhrQueue.getMessageContainingAttribute("conversationId", conversationId));
    }

    @Test
    void shouldPutMessageWithAttachmentsFromInboundActiveMQAndObserveItOnAttachmentsObservabilityQueue() throws JMSException {  //this test would expand and change as progress
        String conversationId = UUID.randomUUID().toString();
        System.out.println("conversation Id " + conversationId);
        mqClient.postAMessageToAQueue("inbound", GetMessageWithUniqueConversationIdAndMessageId("message_with_attachment", conversationId));
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
        String nhsNumber = "9693643038";
        String nemsMessageId = UUID.randomUUID().toString();
        String conversationId = UUID.randomUUID().toString();
        System.out.println("conversation Id " + conversationId);

        String message = "{\"nhsNumber\":\"" + nhsNumber + "\",\"nemsMessageId\":\"" + nemsMessageId + "\",\"nemsEventLastUpdated\":\"" + ZonedDateTime.now(ZoneOffset.ofHours(0)) + "\", \"sourceGp\":\"N82668\",\"destinationGp\":\"B85002\",\"conversationId\":\"" + conversationId + "\"}";
        repoIncomingQueue.postAMessage(message);
        assertThat(ehrCompleteQueue.getMessageContaining(conversationId));
        assertTrue(trackerDb.statusForConversationIdIs(conversationId, "ACTION:EHR_TRANSFER_TO_REPO_COMPLETE"));
    }

    @Test
    void shouldPutANegativeAcknowledgmentOnTransferCompleteQueueWhenReceivedNegativeAcknowledgementInInboundActiveMq() throws JMSException {
        final String REQUESTER_NOT_REGISTERED_PRACTICE_FOR_PATIENT_CODE = "19";

        String conversationId = UUID.randomUUID().toString();
        System.out.println("conversation Id " + conversationId);
        String nemsMessageId = UUID.randomUUID().toString();
        // todo - describe state of patient - known or setup
        String nhsNumber = "9693642937";

        // todo - builder
        // todo - described things, not magic codes
        String message = "{\"nhsNumber\":\"" + nhsNumber + "\",\"nemsMessageId\":\"" + nemsMessageId + "\",\"nemsEventLastUpdated\":\""
                + ZonedDateTime.now(ZoneOffset.ofHours(0)) + "\", "
                + "\"sourceGp\":\"M85019\"," +
                "\"destinationGp\":\"B85002\"," +
                "\"conversationId\":\"" + conversationId + "\"}";

        repoIncomingQueue.postAMessage(message);

        // todo - bring in case sensitivity
        assertThat(negativeAcknowledgementObservabilityQueue.getMessageContaining(conversationId.toUpperCase()));
        assertThat(ehrCompleteQueue.getMessageContaining(conversationId.toUpperCase()));

        // todo - magic number
        // todo - resolve when status becomes resolved, then check code number/state - otherwise hangs around 4 evvaaa
//        assertTrue(trackerDb.statusForConversationIdIs(conversationId, "ACTION:EHR_TRANSFER_FAILED:15"));

        var status = trackerDb.waitForStatusMatching(conversationId, "ACTION:EHR_TRANSFER_FAILED");
        assertThat(status).isEqualTo("ACTION:EHR_TRANSFER_FAILED:" + REQUESTER_NOT_REGISTERED_PRACTICE_FOR_PATIENT_CODE);
    }

    private String GetMessageWithUniqueConversationIdAndMessageId(String fileName, String conversationId) {
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
