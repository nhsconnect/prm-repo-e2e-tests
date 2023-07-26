package uk.nhs.prm.e2etests.performance;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.annotation.EnableScheduling;
import uk.nhs.prm.e2etests.TestConfiguration;
import uk.nhs.prm.e2etests.configuration.ResourceConfiguration;
import uk.nhs.prm.e2etests.model.NhsNumberTestData;
import uk.nhs.prm.e2etests.property.NhsProperties;
import uk.nhs.prm.e2etests.property.PdsAdaptorProperties;
import uk.nhs.prm.e2etests.mesh.MeshMailbox;
import uk.nhs.prm.e2etests.pdsadaptor.PdsAdaptorClient;
import uk.nhs.prm.e2etests.performance.load.*;
import uk.nhs.prm.e2etests.queue.ForceXercesParserSoLogbackDoesNotBlowUpWhenUsingSwiftMqClient;
import uk.nhs.prm.e2etests.queue.SqsMessage;
import uk.nhs.prm.e2etests.suspensions.MofUpdatedMessageQueue;

import java.time.LocalDateTime;
import java.util.List;

import static java.lang.System.out;
import static java.time.LocalDateTime.now;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static uk.nhs.prm.e2etests.nhs.NhsIdentityGenerator.randomNemsMessageId;
import static uk.nhs.prm.e2etests.nhs.NhsIdentityGenerator.randomNhsNumber;
import static uk.nhs.prm.e2etests.performance.NemsTestEvent.nonSuspensionEvent;
import static uk.nhs.prm.e2etests.performance.load.LoadPhase.atFlatRate;
import static uk.nhs.prm.e2etests.performance.reporting.PerformanceChartGenerator.generateProcessingDurationScatterPlot;
import static uk.nhs.prm.e2etests.performance.reporting.PerformanceChartGenerator.generateThroughputPlot;

@SpringBootTest
@ExtendWith(ForceXercesParserSoLogbackDoesNotBlowUpWhenUsingSwiftMqClient.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnableScheduling
public class PerformanceTest {
    // CONSTANTS
    public static final int TOTAL_MESSAGES_PER_DAY = 17000;
    public static final int SUSPENSION_MESSAGES_PER_DAY = 4600;
    public static final int NON_SUSPENSION_MESSAGES_PER_DAY = TOTAL_MESSAGES_PER_DAY - SUSPENSION_MESSAGES_PER_DAY;
    public static final int THROUGHPUT_BUCKET_SECONDS = 60;

    // BEANS
    private MeshMailbox meshMailbox;
    private TestConfiguration testConfiguration;
    private NhsNumberTestData nhsNumbers;
    private MofUpdatedMessageQueue mofUpdatedMessageQueue;
    private PdsAdaptorProperties pdsAdaptorProperties;
    private NhsProperties nhsProperties;

    @Autowired
    public PerformanceTest(
            MeshMailbox meshMailbox,
            TestConfiguration testConfiguration,
            ResourceConfiguration resourceConfiguration,
            MofUpdatedMessageQueue mofUpdatedMessageQueue,
            PdsAdaptorProperties pdsAdaptorProperties,
            NhsProperties nhsProperties
    ) {
        this.meshMailbox = meshMailbox;
        this.testConfiguration = testConfiguration;
        this.nhsNumbers = resourceConfiguration.nhsNumbers();
        this.mofUpdatedMessageQueue = mofUpdatedMessageQueue;
        this.pdsAdaptorProperties = pdsAdaptorProperties;
        this.nhsProperties = nhsProperties;
    }

    @Disabled("only used for perf test development not wanted on actual runs")
    @Test
    public void shouldMoveSingleSuspensionMessageFromNemsToMofUpdatedQueue() {
        var nhsNumberPool = new RoundRobinPool<>(nhsNumbers.getNhsNumbers());
        var suspensions = new SuspensionCreatorPool(nhsNumberPool);

        var nemsEvent = injectSingleNemsSuspension(new DoNothingTestEventListener(), suspensions.next());

        out.println("looking for message containing: " + nemsEvent.nemsMessageId());

        var successMessage = mofUpdatedMessageQueue.getMessageContaining(nemsEvent.nemsMessageId());

        assertThat(successMessage).isNotNull();

        nemsEvent.finished(successMessage);
    }

    @Test
    public void testAllSuspensionMessagesAreProcessedWhenLoadedWithProfileOfRatesAndInjectedMessageCounts() {
        final int overallTimeout = testConfiguration.getPerformanceTestTimeout();
        final var recorder = new PerformanceTestRecorder();

        var eventSource = createMixedSuspensionsAndNonSuspensionsTestEventSource(SUSPENSION_MESSAGES_PER_DAY, NON_SUSPENSION_MESSAGES_PER_DAY);
        var loadSource = new LoadRegulatingPool<>(eventSource, testConfiguration.performanceTestLoadPhases(List.<LoadPhase>of(
                atFlatRate(10, "1"),
                atFlatRate(10, "2"))));

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

        String meshMessageId = meshMailbox.postMessage(nemsSuspension);

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
        List<String> suspendedNhsNumbers = nhsNumbers.getNhsNumbers();
        checkSuspended(suspendedNhsNumbers);
        return new RoundRobinPool(suspendedNhsNumbers);
    }

    private void checkSuspended(List<String> suspendedNhsNumbers) {
        if (!nhsProperties.getNhsEnvironment().equals("perf")) {
            PdsAdaptorClient pds = new PdsAdaptorClient("performance-test", pdsAdaptorProperties.getPerformanceApiKey(), pdsAdaptorProperties.getPdsAdaptorUrl());
            for (String nhsNumber: suspendedNhsNumbers) {
                var patientStatus = pds.getSuspendedPatientStatus(nhsNumber);
                out.println(nhsNumber + ": " + patientStatus);
                assertThat(patientStatus.getIsSuspended()).isTrue();
            }
        }
    }

    private boolean before(LocalDateTime timeout) {
        return now().isBefore(timeout);
    }
}
