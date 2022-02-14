package uk.nhs.prm.deduction.e2e.performance;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.annotation.EnableScheduling;
import uk.nhs.prm.deduction.e2e.TestConfiguration;
import uk.nhs.prm.deduction.e2e.deadletter.NemsEventProcessorDeadLetterQueue;
import uk.nhs.prm.deduction.e2e.mesh.MeshMailbox;
import uk.nhs.prm.deduction.e2e.nems.MeshForwarderQueue;
import uk.nhs.prm.deduction.e2e.nems.NemsEventProcessorUnhandledQueue;
import uk.nhs.prm.deduction.e2e.pdsadaptor.PdsAdaptorClient;
import uk.nhs.prm.deduction.e2e.performance.load.*;
import uk.nhs.prm.deduction.e2e.queue.SqsMessage;
import uk.nhs.prm.deduction.e2e.queue.SqsQueue;
import uk.nhs.prm.deduction.e2e.suspensions.MofNotUpdatedMessageQueue;
import uk.nhs.prm.deduction.e2e.suspensions.MofUpdatedMessageQueue;
import uk.nhs.prm.deduction.e2e.suspensions.NemsEventProcessorSuspensionsMessageQueue;
import uk.nhs.prm.deduction.e2e.suspensions.SuspensionServiceNotReallySuspensionsMessageQueue;
import uk.nhs.prm.deduction.e2e.utility.Helper;

import java.time.LocalDateTime;
import java.util.List;

import static java.lang.System.out;
import static java.time.LocalDateTime.now;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static uk.nhs.prm.deduction.e2e.nhs.NhsIdentityGenerator.randomNemsMessageId;
import static uk.nhs.prm.deduction.e2e.nhs.NhsIdentityGenerator.randomNhsNumber;
import static uk.nhs.prm.deduction.e2e.performance.NemsTestEvent.nonSuspensionEvent;
import static uk.nhs.prm.deduction.e2e.performance.load.LoadPhase.atFlatRate;
import static uk.nhs.prm.deduction.e2e.performance.reporting.PerformanceChartGenerator.generateProcessingDurationScatterPlot;
import static uk.nhs.prm.deduction.e2e.performance.reporting.PerformanceChartGenerator.generateThroughputPlot;

@SpringBootTest(classes = {
        PerformanceTest.class,
        MeshMailbox.class,
        SqsQueue.class,
        TestConfiguration.class,
        MeshForwarderQueue.class,
        NemsEventProcessorUnhandledQueue.class,
        NemsEventProcessorSuspensionsMessageQueue.class,
        SuspensionServiceNotReallySuspensionsMessageQueue.class,
        NemsEventProcessorDeadLetterQueue.class,
        MeshForwarderQueue.class,
        Helper.class,
        MofUpdatedMessageQueue.class,
        MofNotUpdatedMessageQueue.class,
        ScheduledAssumeRoleClient.class
})

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnableScheduling
public class PerformanceTest {

    public static final int TOTAL_MESSAGES_PER_DAY = 17000;
    public static final int SUSPENSION_MESSAGES_PER_DAY = 4600;
    public static final int NON_SUSPENSION_MESSAGES_PER_DAY = TOTAL_MESSAGES_PER_DAY - SUSPENSION_MESSAGES_PER_DAY;
    public static final int THROUGHPUT_BUCKET_SECONDS = 60;

    @Autowired
    private MofUpdatedMessageQueue mofUpdatedMessageQueue;
    @Autowired
    private MeshMailbox meshMailbox;
    @Autowired
    private TestConfiguration config;

    @Disabled("only used for perf test development not wanted on actual runs")
    @Test
    public void shouldMoveSingleSuspensionMessageFromNemsToMofUpdatedQueue() {
        var nhsNumberPool = new RoundRobinPool<>(config.suspendedNhsNumbers());
        var suspensions = new SuspensionCreatorPool(nhsNumberPool);

        var nemsEvent = injectSingleNemsSuspension(new DoNothingTestEventListener(), suspensions.next());

        out.println("looking for message containing: " + nemsEvent.nemsMessageId());

        var successMessage = mofUpdatedMessageQueue.getMessageContaining(nemsEvent.nemsMessageId());

        assertThat(successMessage).isNotNull();

        nemsEvent.finished(successMessage);
    }

    @Test
    public void testAllSuspensionMessagesAreProcessedWhenLoadedWithProfileOfRatesAndInjectedMessageCounts() {
        final int overallTimeout = config.performanceTestTimeout();
        final var recorder = new PerformanceTestRecorder();

        var eventSource = createMixedSuspensionsAndNonSuspensionsTestEventSource(SUSPENSION_MESSAGES_PER_DAY, NON_SUSPENSION_MESSAGES_PER_DAY);
        var loadSource = new LoadRegulatingPool<>(eventSource, config.performanceTestLoadPhases(List.<LoadPhase>of(
                atFlatRate("1", 10),
                atFlatRate("2", 10))));

        var suspensionsOnlyRecorder = new SuspensionsOnlyEventListener(recorder);
        while (loadSource.unfinished()) {
            injectSingleNemsSuspension(suspensionsOnlyRecorder, loadSource.next());
        }

        loadSource.summariseTo(out);

        out.println("Checking mof updated message queue...");

        try {
            final var timeout = now().plusSeconds(overallTimeout);
            while (before(timeout) && recorder.hasUnfinishedEvents()) {
                for (SqsMessage nextMessage : mofUpdatedMessageQueue.getNextMessages(timeout)) {
                    recorder.finishMatchingMessage(nextMessage);
                }
            }
        }
        finally {
            recorder.summariseTo(out);

            generateProcessingDurationScatterPlot(recorder, "Suspension event processing durations vs start time (non-suspensions not shown)");
            generateThroughputPlot(recorder, THROUGHPUT_BUCKET_SECONDS, "Suspension event mean throughput per second in " + THROUGHPUT_BUCKET_SECONDS + " second buckets");
        }

        assertThat(recorder.hasUnfinishedEvents()).isFalse();
    }

    private NemsTestEvent injectSingleNemsSuspension(NemsTestEventListener listener, NemsTestEvent testEvent) {
        var nemsSuspension = testEvent.createMessage();

        listener.onStartingTestItem(testEvent);

        String meshMessageId = meshMailbox.postMessage(nemsSuspension, false);

        testEvent.started(meshMessageId);

        listener.onStartedTestItem(testEvent);

        return testEvent;
    }

    private MixerPool<NemsTestEvent> createMixedSuspensionsAndNonSuspensionsTestEventSource(int suspensionMessagesPerDay, int nonSuspensionMessagesPerDay) {
        var suspensionsSource = new SuspensionCreatorPool(suspendedNhsNumbers());
        var nonSuspensionsSource = new BoringNemsTestEventPool(nonSuspensionEvent(randomNhsNumber(), randomNemsMessageId()));
        return new MixerPool<>(
                suspensionMessagesPerDay, suspensionsSource,
                nonSuspensionMessagesPerDay, nonSuspensionsSource);
    }

    private RoundRobinPool<String> suspendedNhsNumbers() {
        List<String> suspendedNhsNumbers = config.suspendedNhsNumbers();
        checkSuspended(suspendedNhsNumbers);
        return new RoundRobinPool(suspendedNhsNumbers);
    }

    private void checkSuspended(List<String> suspendedNhsNumbers) {
        PdsAdaptorClient pds = new PdsAdaptorClient("performance-test", config.getPdsAdaptorPerformanceApiKey(), config.getPdsAdaptorUrl());
        for (String nhsNumber: suspendedNhsNumbers) {
            var patientStatus = pds.getSuspendedPatientStatus(nhsNumber);
            out.println(nhsNumber + ": " + patientStatus);
            assertThat(patientStatus.getIsSuspended()).isTrue();
        }
    }

    private boolean before(LocalDateTime timeout) {
        return now().isBefore(timeout);
    }
}
