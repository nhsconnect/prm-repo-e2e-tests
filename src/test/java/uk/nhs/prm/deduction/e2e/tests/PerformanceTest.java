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
import uk.nhs.prm.deduction.e2e.performance.DoNothingTestListener;
import uk.nhs.prm.deduction.e2e.performance.NemsPatientEventTestListener;
import uk.nhs.prm.deduction.e2e.performance.RecordingNemsPatientEventTestListener;
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
    @Autowired
    private TestConfiguration config;

    //    TODO
    //    add test for non suspended route/journey
    //    run performance test in the pipeline
    //    reporting! :)
    //    Note: 17,000 a day (X3 for the test - so 51,000); out of the 17k messages 4600 are suspension messages
    @Test
    public void shouldMoveSingleSuspensionMessageFromNemsToMofUpdatedQueue() throws Exception {
        var nhsNumberPool = new RoundRobinList(config.nhsNumbers());

        var nemsMessageId = injectSingleNemsSuspension(nhsNumberPool, new DoNothingTestListener());

        var message = mofUpdatedMessageQueue.getMessageContaining(nemsMessageId);

        assertThat(message).isNotNull();
    }

    private String injectSingleNemsSuspension(RoundRobinList nhsNumberPool, NemsPatientEventTestListener listener) throws Exception {
        var nemsMessageId = helper.randomNemsMessageId();
        var nhsNumber = nhsNumberPool.next();
        var previousGP = PdsAdaptorTest.generateRandomOdsCode();

        listener.onStartingTestItem(nemsMessageId, nhsNumber);

        var nemsSuspension = helper.createNemsEventFromTemplate("change-of-gp-suspension.xml",
                nhsNumber,
                nemsMessageId,
                previousGP);
        meshMailbox.postMessage(nemsSuspension);

        listener.onStartedTestItem(nemsMessageId, nhsNumber);
        return nemsMessageId;
    }

    @Test
    public void testInjectingSuspensionMessagesAtExpectedRateThenAtHigherRate__NotYetCheckingCompletion() throws InterruptedException {
        final var recorder = new RecordingNemsPatientEventTestListener();
        final var maxItemsToBeProcessed = 100;
        final var timeoutInSeconds = 30;

        var nhsNumberPool = new RoundRobinList(config.nhsNumbers());
        var timerTask = new TimerTask() {
            public void run() {
                try {
                    injectSingleNemsSuspension(nhsNumberPool, recorder);

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

        while (recorder.testItemCount() <= maxItemsToBeProcessed) {
            var secondsElapsed = ChronoUnit.SECONDS.between(executionStartTime, LocalDateTime.now());
            if (secondsElapsed >= timeoutInSeconds) {
                System.out.println("Timeout! Shutting down tasks");
                break;
            }
            Thread.sleep(1000);
        }
        slowerRateExecutor.shutdown();
        fasterRateExecutor.shutdown();

        System.out.println("Number of items processed: " + recorder.testItemCount());
        System.out.println("Will check if they went through the system...");

        assertThat(true);
    }

    private ScheduledExecutorService triggerTasksExecution(long startAfterDelayOf, long delayBetweenRuns, TimerTask task) {
        final var executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(task, startAfterDelayOf, delayBetweenRuns, TimeUnit.MILLISECONDS);
        return executor;
    }

}
