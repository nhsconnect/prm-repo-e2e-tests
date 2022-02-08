package uk.nhs.prm.deduction.e2e.performance;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.nhs.prm.deduction.e2e.TestConfiguration;
import uk.nhs.prm.deduction.e2e.deadletter.NemsEventProcessorDeadLetterQueue;
import uk.nhs.prm.deduction.e2e.mesh.MeshMailbox;
import uk.nhs.prm.deduction.e2e.nems.MeshForwarderQueue;
import uk.nhs.prm.deduction.e2e.nems.NemsEventProcessorUnhandledQueue;
import uk.nhs.prm.deduction.e2e.pdsadaptor.PdsAdaptorClient;
import uk.nhs.prm.deduction.e2e.performance.load.LoadPhase;
import uk.nhs.prm.deduction.e2e.performance.load.LoadRegulatingPool;
import uk.nhs.prm.deduction.e2e.performance.load.SuspensionCreatorPool;
import uk.nhs.prm.deduction.e2e.performance.reporting.ScatterPlotGenerator;
import uk.nhs.prm.deduction.e2e.queue.SqsMessage;
import uk.nhs.prm.deduction.e2e.queue.SqsQueue;
import uk.nhs.prm.deduction.e2e.suspensions.MofNotUpdatedMessageQueue;
import uk.nhs.prm.deduction.e2e.suspensions.MofUpdatedMessageQueue;
import uk.nhs.prm.deduction.e2e.suspensions.NemsEventProcessorSuspensionsMessageQueue;
import uk.nhs.prm.deduction.e2e.suspensions.SuspensionServiceNotReallySuspensionsMessageQueue;
import uk.nhs.prm.deduction.e2e.utility.Helper;

import java.time.LocalDateTime;
import java.util.List;

import static java.time.LocalDateTime.now;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static uk.nhs.prm.deduction.e2e.performance.load.LoadPhase.atFlatRate;

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
        MofNotUpdatedMessageQueue.class
})

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PerformanceTest {

    private static final double LOAD_FRACTION_OF_SUSPENSIONS = 4600 / 17000;
    @Autowired
    private MofUpdatedMessageQueue mofUpdatedMessageQueue;
    @Autowired
    private MeshMailbox meshMailbox;
    @Autowired
    private Helper helper;
    @Autowired
    private TestConfiguration config;

    //    Note: 17,000 a day (X3 for the test - so 51,000); out of the 17k messages 4600 are suspension messages
    @Test
    public void shouldMoveSingleSuspensionMessageFromNemsToMofUpdatedQueue() throws Exception {
        var nhsNumberPool = new RoundRobinPool<>(config.suspendedNhsNumbers());

        var nemsEvent = injectSingleNemsSuspension(new DoNothingTestListener(), new SuspensionCreatorPool(nhsNumberPool));

        System.out.println("looking for message containing: " + nemsEvent.nemsMessageId());

        var successMessage = mofUpdatedMessageQueue.getMessageContaining(nemsEvent.nemsMessageId());

        assertThat(successMessage).isNotNull();

        nemsEvent.finished(successMessage);
    }

    private NemsTestEvent injectSingleNemsSuspension(NemsPatientEventTestListener listener, SuspensionCreatorPool suspensionSource) {
        NemsTestEvent testEvent = suspensionSource.next();

        var nemsSuspension = testEvent.createMessage();

        listener.onStartingTestItem(testEvent);

        meshMailbox.postMessage(nemsSuspension);

        testEvent.started();

        listener.onStartedTestItem(testEvent);

        return testEvent;
    }

    @Test
    public void testAllSuspensionMessagesAreProcessedWhenLoadedWithProfileOfRatesAndInjectedMessageCounts() {
        final int overallTimeout = config.performanceTestTimeout();
        final var recorder = new RecordingNemsPatientEventTestListener();
        var nhsNumberSource = new LoadRegulatingPool<>(suspendedNhsNumbers(), config.performanceTestLoadPhases(List.<LoadPhase>of(
                atFlatRate("0.2", 20),
                atFlatRate("0.5", 40),
                atFlatRate("1.0", 60),
                atFlatRate("2.0", 120))));

        while (nhsNumberSource.unfinished()) {
            injectSingleNemsSuspension(recorder, new SuspensionCreatorPool(nhsNumberSource));
        }

        nhsNumberSource.summariseTo(System.out);

        System.out.println("Checking mof updated message queue...");

        final var timeout = now().plusSeconds(overallTimeout);
        while (before(timeout) && recorder.hasUnfinishedEvents()) {
            for (SqsMessage nextMessage : mofUpdatedMessageQueue.getNextMessages()) {
                recorder.finishMatchingMessage(nextMessage);
            }
        }

        recorder.summariseTo(System.out);

        ScatterPlotGenerator.generateProcessingDurationScatterPlot(recorder);

        assertThat(recorder.hasUnfinishedEvents()).isFalse();
    }

    private RoundRobinPool<String> suspendedNhsNumbers() {
        List<String> suspendedNhsNumbers = config.suspendedNhsNumbers();
        checkSuspended(suspendedNhsNumbers);
        return new RoundRobinPool(suspendedNhsNumbers);
    }

    private void checkSuspended(List<String> suspendedNhsNumbers) {
        PdsAdaptorClient pds = new PdsAdaptorClient();
        for (String nhsNumber: suspendedNhsNumbers) {
            var patientStatus = pds.getSuspendedPatientStatus(nhsNumber);
            System.out.println(nhsNumber + ": " + patientStatus);
            assertThat(patientStatus.getIsSuspended()).isTrue();
        }
    }

    private boolean before(LocalDateTime timeout) {
        return now().isBefore(timeout);
    }
}