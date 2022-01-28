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

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;

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
//    Note: 17,000 a day (X3 for the test - so 51,000); out of the 17k messages 4600 are suspension messages
    @Disabled("WIP")
    @Test
    public void shouldMoveSuspensionMessageFromNemsToMofNotUpdatedQueue() throws Exception {
        var nhsNumberUnderTest = "9693797396"; // taken from e2e tests
        var nemsMessageId = helper.randomNemsMessageId();
        var nemsSuspension = helper.createNemsEventFromTemplate("change-of-gp-suspension.xml", nhsNumberUnderTest, nemsMessageId);
        meshMailbox.postMessage(nemsSuspension);

        var message = mofNotUpdatedMessageQueue.getMessageContaining(nemsMessageId);

        assertThat(message).isNotNull();
    }

    @Test
    public void buildingUpCodeToBeExecutedAtDifferentRates() throws InterruptedException {
//        final var nhsNumbers = Arrays.asList("one", "two", "three", "four", "five");
        final var nhsNumbers = Arrays.asList("9693797477");
        final var nemsMessageIdToNhsNumberPairs = new Hashtable<>();
        final var maxItemsToBeProcessed = 100;
        final var timeoutInSeconds = 30;

        var timerTask = new TimerTask() {
            public void run() {
                var nemsMessageId = helper.randomNemsMessageId();
                var nhsNumber = getRandomItemFromList(nhsNumbers);
                nemsMessageIdToNhsNumberPairs.put(nemsMessageId, nhsNumber);
                try {
                    var nemsSuspension = helper.createNemsEventFromTemplate("change-of-gp-suspension.xml", nhsNumber, nemsMessageId);
                    meshMailbox.postMessage(nemsSuspension);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                System.out.println("Task performed on " + new Date() + " " + nemsMessageId + " " + nhsNumber);
            }
        };

        final var executionStartTime = LocalDateTime.now();
        final var slowerRateExecutor = triggerTasksExecution(0,1000, timerTask);
        final var fasterRateExecutor = triggerTasksExecution(5000, 10, timerTask);

        while (nemsMessageIdToNhsNumberPairs.size() <= maxItemsToBeProcessed) {
            var secondsElapsed = ChronoUnit.SECONDS.between(executionStartTime, LocalDateTime.now());
            if (secondsElapsed >= timeoutInSeconds) {
                System.out.println("Timeout! Shutting down tasks");
                break;
            }
            Thread.sleep(1000);
        }

        slowerRateExecutor.shutdown();
        fasterRateExecutor.shutdown();

        System.out.println("Number of items processed: " + nemsMessageIdToNhsNumberPairs.size());
        System.out.println("Will check if they went through the system...");

        assertThat(true);
    }

    private ScheduledExecutorService triggerTasksExecution(long startAfterDelayOf, long delayBetweenRuns, TimerTask task) {
        final var executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(task, startAfterDelayOf, delayBetweenRuns, TimeUnit.MILLISECONDS);
        return executor;
    }

    private String getRandomItemFromList(List<String> list) {
        var rand = new Random();
        return list.get(rand.nextInt(list.size()));
    }
}
