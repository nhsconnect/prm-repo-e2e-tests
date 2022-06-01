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
        TransferCompleteQueue.class
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
    TransferCompleteQueue transferCompleteQueue;
    @BeforeAll
    void init() {
        smallEhrQueue.deleteAllMessages();
        largeEhrQueue.deleteAllMessages();
        attachmentQueue.deleteAllMessages();
        parsingDLQ.deleteAllMessages();
    }

    @Test
    void shouldTestThatMessagesAreReadCorrectlyFromRepoIncomingQueueAndAnEhrRequestIsMadeAndTheDbIsUpdatedWithExpectedStatus() {  //this test would expand and change as progress
        String nhsNumber = "9693795989";
        String nemsMessageId = UUID.randomUUID().toString();
        String conversationId = UUID.randomUUID().toString();
        postMessageToRepoIncomingQueue(nemsMessageId, conversationId, nhsNumber);
        assertTrue(trackerDb.conversationIdExists(conversationId));
        assertTrue(trackerDb.statusForConversationIdIs(conversationId, "ACTION:EHR_REQUEST_SENT"));
    }

    private void postMessageToRepoIncomingQueue(String nemsMessageId, String conversationId, String nhsNumber) {
        String message = "{\"nhsNumber\":\"" + nhsNumber + "\",\"nemsMessageId\":\"" + nemsMessageId + "\", \"sourceGp\":\"N82668\",\"destinationGp\":\"B85002\",\"conversationId\":\"" + conversationId + "\"}";
        repoIncomingQueue.postAMessage(message);
        System.out.println("Sent message to repo incoming queue");
    }

    @Test
    void shouldReadMessageFromActiveMQProcessAndPutItOnSmallEhrAndEhrCompleteAndTransferCompleteQueues() throws JMSException {  //this test would expand and change as progress
        String conversationId = UUID.randomUUID().toString();
        postMessageToRepoIncomingQueue(UUID.randomUUID().toString(), conversationId, "1234567890");
        System.out.println("conversation Id " + conversationId);
        mqClient.postAMessageToAQueue("inbound", GetMessageWithUniqueConversationIdAndMessageId("unsanitized_small_ehr", conversationId));

        assertThat(smallEhrQueue.getMessageContaining(conversationId));
        assertThat(ehrCompleteQueue.getMessageContaining(conversationId));
        assertThat(transferCompleteQueue.getMessageContainingAttribute("conversationId", conversationId));
    }

    @Test
    void shouldPutLargeEhrFromActiveMQAndObserveItOnLargeEhrObservabilityQueue() throws JMSException {  //this test would expand and change as progress
        String conversationId = UUID.randomUUID().toString();
        System.out.println("conversation Id " + conversationId);
        mqClient.postAMessageToAQueue("inbound", GetMessageWithUniqueConversationIdAndMessageId("unsanitized_large_ehr", conversationId));
        assertThat(largeEhrQueue.getMessageContainingAttribute("conversationId", conversationId));
    }

    @Test
    void shouldPutMessageWithAttachmentsFromActiveMQAndObserveItOnAttachmentsObservabilityQueue() throws JMSException {  //this test would expand and change as progress
        String conversationId = UUID.randomUUID().toString();
        System.out.println("conversation Id " + conversationId);
        mqClient.postAMessageToAQueue("inbound", GetMessageWithUniqueConversationIdAndMessageId("message_with_attachment", conversationId));
        assertThat(attachmentQueue.getMessageContaining(conversationId));
    }

    @Test
    void shouldPutAUnprocessableMessageFromActiveMqToDLQ() throws JMSException {  //this test would expand and change as progress
        String dlqMessage = "A DLQ MESSAGE";
        System.out.println("dlq message " + dlqMessage);
        mqClient.postAMessageToAQueue("inbound", dlqMessage);
        assertThat(parsingDLQ.getMessageContaining(dlqMessage));
    }

    private String GetMessageWithUniqueConversationIdAndMessageId(String fileName, String conversationId) {
        String messageId = UUID.randomUUID().toString();
        String message = Resources.readTestResourceFileFromEhrDirectory(fileName);
        message = message.replaceAll("__conversationId__", conversationId);
        message = message.replaceAll("__messageId__", messageId);
        return message;
    }
}
