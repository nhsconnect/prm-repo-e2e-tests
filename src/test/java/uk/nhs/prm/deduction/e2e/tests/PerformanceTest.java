package uk.nhs.prm.deduction.e2e.tests;

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
    private MofUpdatedMessageQueue mofUpdatedMessageQueue;
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
//    Watch out for auth token
//    @Disabled("WIP")
    @Test
    public void shouldMoveSuspensionMessageFromNemsToMofUpdatedQueue() throws Exception {
        var nhsNumberUnderTest = "9693797396";
        var nemsMessageId = helper.randomNemsMessageId();
        var previousGP = PdsAdaptorTest.generateRandomOdsCode();
        var nemsSuspension = helper.createNemsEventFromTemplate(
                "change-of-gp-suspension.xml",
                nhsNumberUnderTest,
                nemsMessageId,
                previousGP);
        meshMailbox.postMessage(nemsSuspension);

        var message = mofUpdatedMessageQueue.getMessageContaining(nemsMessageId);

        assertThat(message).isNotNull();
    }

    @Test
    public void buildingUpCodeToBeExecutedAtDifferentRates() throws InterruptedException {
//        final var nhsNumbers = Arrays.asList("one", "two", "three", "four", "five");
        final var nhsNumbers = Arrays.asList("9693797477", "9693797396");
        final var nemsMessageIdToNhsNumberPairs = new Hashtable<>();
        final var maxItemsToBeProcessed = 100;
        final var timeoutInSeconds = 30;

        final String[] lastNhsNumber = { nhsNumbers.get(0) };
        var timerTask = new TimerTask() {
            public void run() {
                try {
                    System.out.println("starting run()");
                    var nemsMessageId = helper.randomNemsMessageId();
                    var nhsNumber = getNextRoundRobinItem(nhsNumbers, lastNhsNumber[0]);
                    lastNhsNumber[0] = nhsNumber;
                    nemsMessageIdToNhsNumberPairs.put(nemsMessageId, nhsNumber);
                    var nemsSuspension = helper.createNemsEventFromTemplate("change-of-gp-suspension.xml", nhsNumber, nemsMessageId);
                    meshMailbox.postMessage(nemsSuspension);
                    System.out.println("Task performed on " + new Date() + " " + nemsMessageId + " " + nhsNumber);
                } catch (Exception e) {
                    System.out.println("Failed single run()");
                    System.out.println(e.getMessage());
                    e.printStackTrace();
                }
            }
        };

        final var executionStartTime = LocalDateTime.now();
        final var slowerRateExecutor = triggerTasksExecution(0, 5000, timerTask);
        final var fasterRateExecutor = triggerTasksExecution(5000, 1000, timerTask);

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

    private String getNextRoundRobinItem(List<String> list, String item) {
        int index = list.indexOf(item);
        if (index < list.size() - 1) {
            index += 1;
        } else {
            index = 0;
        }
        return list.get(index);
    }
}
