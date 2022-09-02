package uk.nhs.prm.deduction.e2e.performance;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
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
import uk.nhs.prm.deduction.e2e.transfer_tracker_db.DbClient;
import uk.nhs.prm.deduction.e2e.transfer_tracker_db.TrackerDb;

import java.util.ArrayList;

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
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RepoInPerformanceTest {
    @Autowired
    RepoIncomingQueue repoIncomingQueue;

    @Autowired
    TestConfiguration config;

    @Autowired
    TrackerDb trackerDb;

    @Test
    public void trackBehaviourOfHighNumberOfMessagesSentToEhrTransferService() {
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

        // ... ensure all is in tracker db? Or countdown on the queue?
        repoIncomingMessages.forEach(message ->
            assertTrue(trackerDb.statusForConversationIdIs(message.conversationId(), "ACTION:TRANSFER_TO_REPO_STARTED", 300))
        );

//        (after all messages sent) Send small EHR message (~4Mb) to ActiveMQ MHS inbound queue via AMQP with corresponding conversation id

        // shall we assert on being the records at the other end - transfer complete observability
    }
}
