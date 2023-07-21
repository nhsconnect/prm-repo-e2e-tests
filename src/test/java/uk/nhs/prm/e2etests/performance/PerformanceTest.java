package uk.nhs.prm.e2etests.performance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;
import uk.nhs.prm.e2etests.TestConfiguration;
import uk.nhs.prm.e2etests.configuration.MeshPropertySource;
import uk.nhs.prm.e2etests.configuration.PdsAdaptorPropertySource;
import uk.nhs.prm.e2etests.configuration.QueuePropertySource;
import uk.nhs.prm.e2etests.mesh.MeshMailbox;
import uk.nhs.prm.e2etests.pdsadaptor.PdsAdaptorClient;
import uk.nhs.prm.e2etests.performance.awsauth.AssumeRoleCredentialsProviderFactory;
import uk.nhs.prm.e2etests.performance.awsauth.AutoRefreshingRoleAssumingSqsClient;
import uk.nhs.prm.e2etests.performance.load.*;
import uk.nhs.prm.e2etests.queue.BasicSqsClient;
import uk.nhs.prm.e2etests.queue.ForceXercesParserSoLogbackDoesNotBlowUpWhenUsingSwiftMqClient;
import uk.nhs.prm.e2etests.queue.SqsMessage;
import uk.nhs.prm.e2etests.queue.ThinlyWrappedSqsClient;
import uk.nhs.prm.e2etests.suspensions.MofUpdatedMessageQueue;
import uk.nhs.prm.e2etests.utility.QueueHelper;

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
    private TestConfiguration config;
    private MofUpdatedMessageQueue mofUpdatedMessageQueue;
    PdsAdaptorPropertySource pdsAdaptorPropertySource;

    @Autowired
    public PerformanceTest(
            MeshMailbox meshMailbox,
            TestConfiguration config,
            MofUpdatedMessageQueue mofUpdatedMessageQueue,
            PdsAdaptorPropertySource pdsAdaptorPropertySource
    ) {
        this.meshMailbox = meshMailbox;
        this.config = config;
        this.mofUpdatedMessageQueue = mofUpdatedMessageQueue;
        this.pdsAdaptorPropertySource = pdsAdaptorPropertySource;
    }

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
        List<String> suspendedNhsNumbers = config.suspendedNhsNumbers();
        checkSuspended(suspendedNhsNumbers);
        return new RoundRobinPool(suspendedNhsNumbers);
    }

    private void checkSuspended(List<String> suspendedNhsNumbers) {
        if (!config.getEnvironmentName().equals("perf")) {
            PdsAdaptorClient pds = new PdsAdaptorClient("performance-test", pdsAdaptorPropertySource.getPerformanceApiKey(), pdsAdaptorPropertySource.getPdsAdaptorUrl());
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

//    private AutoRefreshingRoleAssumingSqsClient appropriateAuthenticationSqsClient() {
//        if (config.performanceTestTimeout() > TestConfiguration.SECONDS_IN_AN_HOUR * 0.9) {
//            var authStrategyWarning = "Performance test timeout is approaching an hour, getting where this will not work if " +
//                    "using temporary credentials (such as obtained by user using MFA) if it exceeds the expiration time. " +
//                    "Longer runs will need to be done in pipeline where refresh can be made from the AWS instance's " +
//                    "metadata credentials lookup.";
//            System.err.println(authStrategyWarning);
//        }
//        out.println("AUTH STRATEGY: using auto-refresh, role-assuming sqs client");
//        return context.getBean(AutoRefreshingRoleAssumingSqsClient.class);
//    }
}
