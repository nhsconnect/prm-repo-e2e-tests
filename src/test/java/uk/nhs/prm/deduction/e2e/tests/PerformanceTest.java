package uk.nhs.prm.deduction.e2e.tests;

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
import uk.nhs.prm.deduction.e2e.performance.*;
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
import static uk.nhs.prm.deduction.e2e.performance.LoadPhase.atFlatRate;

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
        var nhsNumberPool = new RoundRobinPool<>(config.suspendedNhsNumbers());

        var nemsEvent = injectSingleNemsSuspension(nhsNumberPool.next(), new DoNothingTestListener());

        System.out.println("looking for message containing: " + nemsEvent.nemsMessageId());

        var successMessage = mofUpdatedMessageQueue.getMessageContaining(nemsEvent.nemsMessageId());

        assertThat(successMessage).isNotNull();

        nemsEvent.finished(successMessage);
    }

    private NemsTestEvent injectSingleNemsSuspension(String nhsNumber, NemsPatientEventTestListener listener) {
        var nemsMessageId = helper.randomNemsMessageId();
        var previousGP = PdsAdaptorTest.generateRandomOdsCode();

        var testEvent = new NemsTestEvent(nemsMessageId, nhsNumber);

        listener.onStartingTestItem(testEvent);

        var nemsSuspension = helper.createNemsEventFromTemplate("change-of-gp-suspension.xml",
                nhsNumber,
                nemsMessageId,
                previousGP);
        meshMailbox.postMessage(nemsSuspension);

        testEvent.started();

        listener.onStartedTestItem(testEvent);

        return testEvent;
    }

    @Test
    public void testInjectingSuspensionMessages___AsFastAsPossible() {
        final var recorder = new RecordingNemsPatientEventTestListener();
        final var maxItemsToBeProcessed = 20;

        var nhsNumberSource = new LoadRegulatingPool<>(suspendedNhsNumbers(),
                List.<LoadPhase>of(atFlatRate(100, "0.2")),
                maxItemsToBeProcessed);

        while (nhsNumberSource.unfinished()) {
            injectSingleNemsSuspension(nhsNumberSource.next(), recorder);
        }

        nhsNumberSource.summariseTo(System.out);

        System.out.println("Checking mof updated message queue...");

        final var timeout = now().plusSeconds(150);
        while (before(timeout) && recorder.hasUnfinishedEvents()) {
            for (SqsMessage nextMessage : mofUpdatedMessageQueue.getNextMessages()) {
                recorder.finishMatchingMessage(nextMessage);
            }
        }

        recorder.summariseTo(System.out);
        ScatterPlotGenerator.generateProcessingDurationScatterPlot(recorder);

        assertThat(recorder.hasUnfinishedEvents()).isFalse();
    }

    private boolean before(LocalDateTime timeout) {
        return now().isBefore(timeout);
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

}
