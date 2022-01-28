package uk.nhs.prm.deduction.e2e.tests;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.nhs.prm.deduction.e2e.TestConfiguration;
import uk.nhs.prm.deduction.e2e.auth.AuthTokenGenerator;
import uk.nhs.prm.deduction.e2e.deadletter.NemsEventProcessorDeadLetterQueue;
import uk.nhs.prm.deduction.e2e.mesh.MeshClient;
import uk.nhs.prm.deduction.e2e.mesh.MeshMailbox;
import uk.nhs.prm.deduction.e2e.nems.MeshForwarderQueue;
import uk.nhs.prm.deduction.e2e.nems.NemsEventProcessorUnhandledQueue;
import uk.nhs.prm.deduction.e2e.pdsadaptor.PdsAdaptorClient;
import uk.nhs.prm.deduction.e2e.queue.SqsQueue;
import uk.nhs.prm.deduction.e2e.suspensions.MofNotUpdatedMessageQueue;
import uk.nhs.prm.deduction.e2e.suspensions.MofUpdatedMessageQueue;
import uk.nhs.prm.deduction.e2e.suspensions.NemsEventProcessorSuspensionsMessageQueue;
import uk.nhs.prm.deduction.e2e.suspensions.SuspensionServiceNotReallySuspensionsMessageQueue;
import uk.nhs.prm.deduction.e2e.utility.Helper;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest(classes = {
        PerformanceTest.class,
        MeshMailbox.class,
        SqsQueue.class,
        MeshClient.class,
        TestConfiguration.class,
        AuthTokenGenerator.class,
        MeshForwarderQueue.class,
        NemsEventProcessorUnhandledQueue.class,
        NemsEventProcessorSuspensionsMessageQueue.class,
        SuspensionServiceNotReallySuspensionsMessageQueue.class,
        NemsEventProcessorDeadLetterQueue.class,
        MeshForwarderQueue.class,
        Helper.class,
        MofUpdatedMessageQueue.class,
        MofNotUpdatedMessageQueue.class,
        PdsAdaptorClient.class
})

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PerformanceTest {

    @Autowired
    private MofNotUpdatedMessageQueue mofNotUpdatedMessageQueue;
    @Autowired
    private MeshMailbox meshMailbox;
    @Autowired
    private Helper helper;

//    TODO
//    find a way to run N runs of test
//    add test for non suspended route/journey
//    run performance test in the pioeline
//    reporting! :)
//    @Disabled("WIP")
    @Test
    public void shouldMoveSuspensionMessageFromNemsToMofNotUpdatedQueue() throws Exception {
        var nhsNumberUnderTest = "9693797396"; // taken from e2e tests
        var nemsMessageId = helper.randomNemsMessageId();
        var nemsSuspension = helper.createNemsEventFromTemplate("change-of-gp-suspension.xml", nhsNumberUnderTest, nemsMessageId);
        meshMailbox.postMessage(nemsSuspension);

        var message = mofNotUpdatedMessageQueue.getMessageContaining(nemsMessageId);

        assertThat(message).isNotNull();
    }

//    @Test
    public void buildingUpCodeToBeExecutedAtDifferentRates() throws InterruptedException {
        System.out.println("Filling Inbox at a slow rate NOW! " + new Date());
        sendMessagesToInboxAtRate(1000, 10000);
        System.out.println("Filling Inbox at a faster rate NOW!" + new Date());
        sendMessagesToInboxAtRate(1, 10000);

        System.out.println("Number of items processed: " + nemsMessageIdToNhsNumberPairs.size());
        System.out.println("Will check if they went through the system...");

        assertThat(true);
    }

    private void sendMessagesToInboxAtRate(long delayBetweenRuns, long forHowLong) throws InterruptedException {
        var task = new TimerTask() {
            public void run() {
                var nemsMessageId = helper.randomNemsMessageId();
                var nhsNumber = randomNhsNumber();
                nemsMessageIdToNhsNumberPairs.put(nemsMessageId, nhsNumber);
                System.out.println("Task performed on " + new Date() + " " + nemsMessageId + " " + nhsNumber);
            }
        };

        var executor = Executors.newSingleThreadScheduledExecutor();
        final long delayForImmediateExecution = 0;
        executor.scheduleAtFixedRate(task, delayForImmediateExecution, delayBetweenRuns, TimeUnit.MILLISECONDS);
        // TODO: stop when N number of items are processed, or a timeout is reached
        Thread.sleep(forHowLong);
        executor.shutdown();
    }

    private final List<String> nhsNumbers = Arrays.asList("one", "two", "three", "four", "five");
    private final Dictionary<String, String> nemsMessageIdToNhsNumberPairs = new Hashtable<>();

    private String randomNhsNumber() {
        var rand = new Random();
        return nhsNumbers.get(rand.nextInt(nhsNumbers.size()));
    }
}
