package uk.nhs.prm.e2etests.tests;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.client.HttpClientErrorException;
import uk.nhs.prm.e2etests.model.ActiveSuspensionsMessage;
import uk.nhs.prm.e2etests.model.nems.DeceasedPatientMessageNems;
import uk.nhs.prm.e2etests.model.nems.MofUpdatedMessageNems;
import uk.nhs.prm.e2etests.model.nems.NemsEventMessage;
import uk.nhs.prm.e2etests.model.nems.NemsResolutionMessage;
import uk.nhs.prm.e2etests.model.nems.NoLongerSuspendedMessageNems;
import uk.nhs.prm.e2etests.queue.suspensions.SuspensionServiceDeceasedPatientQueue;
import uk.nhs.prm.e2etests.queue.suspensions.SuspensionServiceMofNotUpdatedQueue;
import uk.nhs.prm.e2etests.queue.suspensions.SuspensionServiceMofUpdatedQueue;
import uk.nhs.prm.e2etests.queue.suspensions.observability.SuspensionsServiceRepoIncomingOQ;
import uk.nhs.prm.e2etests.queue.nems.observability.NemsEventProcessorSuspensionsOQ;
import uk.nhs.prm.e2etests.queue.suspensions.observability.SuspensionServiceNotSuspendedOQ;
import uk.nhs.prm.e2etests.service.ActiveSuspensionsService;
import uk.nhs.prm.e2etests.enumeration.Gp2GpSystem;
import uk.nhs.prm.e2etests.property.NhsProperties;
import uk.nhs.prm.e2etests.property.PdsAdaptorProperties;
import uk.nhs.prm.e2etests.queue.nems.NemsEventProcessorDeadLetterQueue;
import uk.nhs.prm.e2etests.mesh.MeshMailbox;
import uk.nhs.prm.e2etests.queue.nems.observability.MeshForwarderOQ;
import uk.nhs.prm.e2etests.queue.nems.NemsEventProcessorUnhandledEventsQueue;
import uk.nhs.prm.e2etests.service.PdsAdaptorService;
import uk.nhs.prm.e2etests.property.SyntheticPatientProperties;
import uk.nhs.prm.e2etests.queue.nems.observability.NemsEventProcessorReRegistrationsOQ;
import uk.nhs.prm.e2etests.service.EhrRepositoryService;
import uk.nhs.prm.e2etests.utility.NemsEventFactory;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.nhs.prm.e2etests.utility.NhsIdentityGenerator.*;
import static uk.nhs.prm.e2etests.utility.NemsEventFactory.createNemsEventFromTemplate;

@SpringBootTest
@ExtendWith(ForceXercesParserSoLogbackDoesNotBlowUpWhenUsingSwiftMqClient.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ContinuityE2E {
    @Autowired
    private MeshForwarderOQ meshForwarderOQ;
    @Autowired
    private NemsEventProcessorUnhandledEventsQueue nemsEventProcessorUnhandledEventsQueue;
    @Autowired
    private NemsEventProcessorSuspensionsOQ nemsEventProcessorSuspensionsOQ;
    @Autowired
    private SuspensionServiceNotSuspendedOQ suspensionServiceNotSuspendedOQ;
    @Autowired
    private SuspensionServiceMofUpdatedQueue suspensionServiceMofUpdatedQueue;
    @Autowired
    private SuspensionServiceMofNotUpdatedQueue suspensionServiceMofNotUpdatedQueue;
    @Autowired
    private NemsEventProcessorDeadLetterQueue nemsEventProcessorDeadLetterQueue;
    @Autowired
    private SuspensionServiceDeceasedPatientQueue suspensionServiceDeceasedPatientQueue;
    @Autowired
    private NemsEventProcessorReRegistrationsOQ nemsEventProcessorReRegistrationsOQ;
    @Autowired
    private MeshMailbox meshMailbox;
    @Autowired
    private SyntheticPatientProperties syntheticPatientProperties;
    @Autowired
    private SuspensionsServiceRepoIncomingOQ suspensionsServiceRepoIncomingOQ;
    @Autowired
    private ActiveSuspensionsService activeSuspensionsService;
    @Autowired
    private EhrRepositoryService ehrRepositoryService;
    @Autowired
    private PdsAdaptorProperties pdsAdaptorProperties;
    @Autowired
    private NhsProperties nhsProperties;

    private PdsAdaptorService pdsAdaptorService;

    private final String EMIS_PTL_INT = "N82668";
    private final String SUSPENDED_PATIENT_NHS_NUMBER = "9693796047";

    @BeforeAll
    void init() {
        meshForwarderOQ.deleteAllMessages();
        nemsEventProcessorDeadLetterQueue.deleteAllMessages();
        nemsEventProcessorSuspensionsOQ.deleteAllMessages();
        nemsEventProcessorUnhandledEventsQueue.deleteAllMessages();
        suspensionServiceNotSuspendedOQ.deleteAllMessages();
        nemsEventProcessorReRegistrationsOQ.deleteAllMessages();
        suspensionsServiceRepoIncomingOQ.deleteAllMessages();
        pdsAdaptorService = new PdsAdaptorService(
                "e2e-test",
                pdsAdaptorProperties.getE2eTestApiKey(),
                pdsAdaptorProperties.getPdsAdaptorUrl()
        );
    }

    @Test
    @DisabledIfEnvironmentVariable(named = "UPDATE_MOF_TO_REPO",matches="true")
    @Order(1)
    public void shouldMoveSuspensionMessageFromNemsToMofUpdatedQueue() {
        String nemsMessageId = randomNemsMessageId();
        String suspendedPatientNhsNumber = syntheticPatientProperties.getPatientWithoutGp();
        var now = now();
        String previousGp = generateRandomOdsCode();
        System.out.printf("Generated random ods code for previous gp: %s%n", previousGp);

        NemsEventMessage nemsSuspension = createNemsEventFromTemplate("change-of-gp-suspension.xml", suspendedPatientNhsNumber, nemsMessageId, previousGp, now);
        meshMailbox.postMessage(nemsSuspension);
        MofUpdatedMessageNems expectedMessageOnQueue = new MofUpdatedMessageNems(nemsMessageId, "ACTION:UPDATED_MANAGING_ORGANISATION");

        assertThat(meshForwarderOQ.hasMessage(nemsSuspension.getMessage())); // TODO PRMT-3574 an 'assertThat' without a matching 'isTrue'? I think these instances need a change
        assertThat(suspensionServiceMofUpdatedQueue.hasResolutionMessage(expectedMessageOnQueue));
    }

    @Test
    @DisabledIfEnvironmentVariable(named = "UPDATE_MOF_TO_REPO",matches="false") //The toggle status for repo_process_only_safe_listed_ods_codes is true in dev and test
    public void shouldPutAMessageForASuspendedPatientWithSafeListedODSCodeOnRepoIncomingWhenTheToggleIsTrue() {
        String nemsMessageId = randomNemsMessageId();
        String safeListedOdsCode = EMIS_PTL_INT;
        var now = now();
        NemsEventMessage nemsSuspension = createNemsEventFromTemplate("change-of-gp-suspension.xml", SUSPENDED_PATIENT_NHS_NUMBER, nemsMessageId, safeListedOdsCode, now);

        setManagingOrganisationToEMISOdsCode(SUSPENDED_PATIENT_NHS_NUMBER);

        meshMailbox.postMessage(nemsSuspension);

        assertThat(suspensionsServiceRepoIncomingOQ.getMessageContaining(nemsMessageId));

    }

    @Test
    @Order(2)
    public void shouldMoveSuspensionMessageWherePatientIsNoLongerSuspendedToNotSuspendedQueue() {
        String nemsMessageId = randomNemsMessageId();
        String previousGp = generateRandomOdsCode();
        var now = now();
        String currentlyRegisteredPatientNhsNumber = syntheticPatientProperties.getPatientWithCurrentGp();

        NemsEventMessage nemsSuspension = createNemsEventFromTemplate("change-of-gp-suspension.xml", currentlyRegisteredPatientNhsNumber, nemsMessageId, previousGp, now);

        NoLongerSuspendedMessageNems expectedMessageOnQueue = new NoLongerSuspendedMessageNems(nemsMessageId, "NO_ACTION:NO_LONGER_SUSPENDED_ON_PDS");

        meshMailbox.postMessage(nemsSuspension);
        assertThat(suspensionServiceNotSuspendedOQ.hasResolutionMessage(expectedMessageOnQueue));

    }

    @Test
    @Order(5)
    public void shouldMoveNonSuspensionMessageFromNemsToUnhandledQueue() throws Exception {
        var nemsMessageId = randomNemsMessageId();

        var nemsNonSuspension = createNemsEventFromTemplate(
                "change-of-gp-non-suspension.xml", randomNhsNumber(), nemsMessageId, now());

        meshMailbox.postMessage(nemsNonSuspension);

        assertThat(nemsEventProcessorUnhandledEventsQueue.hasMessage("{\"nemsMessageId\":\"" + nemsMessageId + "\",\"messageStatus\":\"NO_ACTION:NON_SUSPENSION\"}"));
    }

    @Test
    @Order(6)
    public void shouldSendUnprocessableMessagesToDlQ() throws Exception {
        Map<String, NemsEventMessage> dlqMessages = NemsEventFactory.getDLQNemsEventMessages();
        log("Posting DLQ messages");

        for (Map.Entry<String, NemsEventMessage> message : dlqMessages.entrySet()) {
            meshMailbox.postMessage(message.getValue());
            log("Posted " + message.getKey() + " messages");
            assertThat(nemsEventProcessorDeadLetterQueue.hasMessage(message.getValue().getMessage()));
        }
    }

    @Test
    @Order(4)
    @Disabled(" 'process_only_synthetic_or_safe_listed_patients' toggle is set to false across all the environments. ")
    public void shouldMoveNonSyntheticPatientSuspensionMessageFromNemsToMofNotUpdatedQueueWhenToggleOn() {
        String nemsMessageId = randomNemsMessageId();
        String previousGp = generateRandomOdsCode();

        var suspensionTime = now();
        var nemsSuspension = createNemsEventFromTemplate("change-of-gp-suspension.xml",
                syntheticPatientProperties.getNonSyntheticPatientWithoutGp(),
                nemsMessageId, previousGp, suspensionTime);

        meshMailbox.postMessage(nemsSuspension);

        NemsResolutionMessage expectedMessageOnQueue = new NemsResolutionMessage(nemsMessageId, "NO_ACTION:NOT_SYNTHETIC_OR_SAFE_LISTED");

        assertThat(suspensionServiceMofNotUpdatedQueue.hasResolutionMessage(expectedMessageOnQueue));
    }

    @Test
    @Order(3)
    public void shouldMoveDeceasedPatientEventToDeceasedQueue() {
        String nemsMessageId = randomNemsMessageId();
        String patientNhsNumber = syntheticPatientProperties.getDeceasedPatient();
        var eventTime = now();
        String previousGp = generateRandomOdsCode();

        System.out.printf("Generated random ods code for previous gp: %s%n", previousGp);

        var deceasedEvent = createNemsEventFromTemplate("change-of-gp-suspension.xml",
                patientNhsNumber, nemsMessageId, previousGp, eventTime);

        meshMailbox.postMessage(deceasedEvent);

        var expectedMessageOnQueue = new DeceasedPatientMessageNems(nemsMessageId, "NO_ACTION:DECEASED_PATIENT");

        assertThat(suspensionServiceDeceasedPatientQueue.hasResolutionMessage(expectedMessageOnQueue));
    }

    @Test
    @Order(7)
    public void shouldDeleteEhrOfPatientOnTheirReRegistration() throws Exception {
        var nemsMessageId = randomNemsMessageId();
        String patientNhsNumber = syntheticPatientProperties.getPatientWithCurrentGp();
        var reregistrationTime = now();
        storeEhrInRepositoryFor(patientNhsNumber);
        activeSuspensionsService.save(new ActiveSuspensionsMessage(patientNhsNumber,generateRandomOdsCode(), now()));

        var reRegistration = createNemsEventFromTemplate(
                "change-of-gp-re-registration.xml", patientNhsNumber, nemsMessageId, reregistrationTime);

        meshMailbox.postMessage(reRegistration);

        String expectedMessageOnQueue = "{\"nhsNumber\":\"" + patientNhsNumber + "\"," +
                "\"newlyRegisteredOdsCode\":\"B86056\"," +
                "\"nemsMessageId\":\"" + nemsMessageId + "\"," +
                "\"lastUpdated\":\"" + reregistrationTime + "\"}";




        assertThrows(HttpClientErrorException.class, () -> {
            ehrRepositoryService.getEhrResponse(patientNhsNumber);
        });


        assertThat(meshForwarderOQ.hasMessage(reRegistration.getMessage()));
        assertThat(nemsEventProcessorReRegistrationsOQ.hasMessage(expectedMessageOnQueue));
    }

    @Test
    @DisabledIfEnvironmentVariable(named = "UPDATE_MOF_TO_REPO",matches="true")
    void shouldSaveActiveSuspensionInDbWhenMofUpdatedToPreviousGp() {
        String nemsMessageId = randomNemsMessageId();
        String suspendedPatientNhsNumber = syntheticPatientProperties.getPatientWithoutGp();
        var now = now();
        String previousGp = generateRandomOdsCode();
        System.out.printf("Generated random ods code for previous gp: %s%n", previousGp);

        NemsEventMessage nemsSuspension = createNemsEventFromTemplate("change-of-gp-suspension.xml", suspendedPatientNhsNumber, nemsMessageId, previousGp, now);
        meshMailbox.postMessage(nemsSuspension);

        assertTrue(activeSuspensionsService.nhsNumberExists(suspendedPatientNhsNumber));
    }


    private static String now() {
        return ZonedDateTime.now(ZoneOffset.ofHours(0)).toString();
    }

    private void storeEhrInRepositoryFor(String patientNhsNumber) throws Exception {
        ehrRepositoryService.createEhr(patientNhsNumber);
        assertThat(ehrRepositoryService.getEhrResponse(patientNhsNumber)).isEqualTo("200 OK");
    }

    private void setManagingOrganisationToEMISOdsCode(String nhsNumber) {
        var pdsResponse = pdsAdaptorService.getSuspendedPatientStatus(nhsNumber);
        var repoOdsCode = Gp2GpSystem.repoInEnv(nhsProperties.getNhsEnvironment()).odsCode();
        if (repoOdsCode.equals(pdsResponse.getManagingOrganisation())) {
            pdsAdaptorService.updateManagingOrganisation(nhsNumber, EMIS_PTL_INT, pdsResponse.getRecordETag());
        }
    }

    public void log(String message) {
        System.out.println(message);
    }
}
