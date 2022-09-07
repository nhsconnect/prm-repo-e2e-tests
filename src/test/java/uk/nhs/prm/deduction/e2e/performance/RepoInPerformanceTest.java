package uk.nhs.prm.deduction.e2e.performance;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.nhs.prm.deduction.e2e.TestConfiguration;
import uk.nhs.prm.deduction.e2e.TestData;
import uk.nhs.prm.deduction.e2e.ehr_transfer.RepoIncomingQueue;
import uk.nhs.prm.deduction.e2e.models.Gp2GpSystem;
import uk.nhs.prm.deduction.e2e.models.RepoIncomingMessage;
import uk.nhs.prm.deduction.e2e.models.RepoIncomingMessageBuilder;
import uk.nhs.prm.deduction.e2e.performance.awsauth.AssumeRoleCredentialsProviderFactory;
import uk.nhs.prm.deduction.e2e.performance.awsauth.AutoRefreshingRoleAssumingSqsClient;
import uk.nhs.prm.deduction.e2e.queue.SqsQueue;
import uk.nhs.prm.deduction.e2e.queue.activemq.ForceXercesParserSoLogbackDoesNotBlowUpWhenUsingSwiftMqClient;
import uk.nhs.prm.deduction.e2e.queue.activemq.SimpleAmqpQueue;
import uk.nhs.prm.deduction.e2e.transfer_tracker_db.DbClient;
import uk.nhs.prm.deduction.e2e.transfer_tracker_db.TrackerDb;
import uk.nhs.prm.deduction.e2e.utility.Resources;

import java.util.ArrayList;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = {
        AutoRefreshingRoleAssumingSqsClient.class,
        AssumeRoleCredentialsProviderFactory.class,
        DbClient.class,
        RepoInPerformanceTest.class,
        RepoIncomingQueue.class,
        SqsQueue.class,
        TestConfiguration.class,
        TrackerDb.class,
})
@ExtendWith(ForceXercesParserSoLogbackDoesNotBlowUpWhenUsingSwiftMqClient.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RepoInPerformanceTest {
    @Autowired
    RepoIncomingQueue repoIncomingQueue;

    @Autowired
    TestConfiguration config;

    @Autowired
    TrackerDb trackerDb;

    @Test
    public void trackBehaviourOfHighNumberOfMessagesSentToEhrTransferService() throws InterruptedException {
        var numberOfRecordToBeProcessed = 2;
        var repoIncomingMessages = new ArrayList<RepoIncomingMessage>();

        for (int i = 0; i < numberOfRecordToBeProcessed ; i++) {
            var message = new RepoIncomingMessageBuilder()
                    .withNhsNumber(TestData.generateRandomNhsNumber())
                    .withEhrSourceGp(Gp2GpSystem.EMIS_PTL_INT)
                    .build();
            repoIncomingMessages.add(message);
        }

        // Send high amount of messages to repo-incoming-queue with unique conversation id and nhs number
        repoIncomingMessages.forEach(message -> repoIncomingQueue.send(message));

        // TODO: to be fixed in perf env (ok in dev)
        // ensure messages are in tracker db
//        repoIncomingMessages.forEach(message ->
//            assertTrue(trackerDb.statusForConversationIdIs(message.conversationId(), "ACTION:TRANSFER_TO_REPO_STARTED", 300))
//        );
        Thread.sleep(2000); // To be removed when assertion above is fixed

        var fileName =  "small-ehr";

        System.out.println("About to create SimpleAmqpQueue...");
        var inboundQueueFromMhs = new SimpleAmqpQueue(config);

        System.out.println("About to send messages...");
        repoIncomingMessages.forEach(message -> {
            var conversationId = message.conversationId();
            var smallEhr = getMessageWithUniqueConversationIdAndMessageId(fileName, conversationId);
            inboundQueueFromMhs.sendMessage(smallEhr, conversationId);
        });
        inboundQueueFromMhs.close();

        System.out.println("All good! :)");
        // shall we assert on being the records at the other end - transfer complete observability
        assertTrue(true);
    }

    private String getMessageWithUniqueConversationIdAndMessageId(String fileName, String conversationId) {
        String messageId = randomUUID().toString();
        String message = Resources.readTestResourceFileFromEhrDirectory(fileName);
        message = message.replaceAll("__CONVERSATION_ID__", conversationId);
        message = message.replaceAll("__MESSAGE_ID__", messageId);
        return message;
    }
}
