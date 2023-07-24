package uk.nhs.prm.e2etests.tests;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.client.HttpClientErrorException;
import uk.nhs.prm.e2etests.TestConfiguration;
import uk.nhs.prm.e2etests.active_suspensions_db.ActiveSuspensionsDB;
import uk.nhs.prm.e2etests.property.NhsProperties;
import uk.nhs.prm.e2etests.property.PdsAdaptorProperties;
import uk.nhs.prm.e2etests.deadletter.NemsEventProcessorDeadLetterQueue;
import uk.nhs.prm.e2etests.mesh.MeshMailbox;
import uk.nhs.prm.e2etests.model.*;
import uk.nhs.prm.e2etests.nems.MeshForwarderQueue;
import uk.nhs.prm.e2etests.model.NemsEventMessage;
import uk.nhs.prm.e2etests.nems.NemsEventProcessorUnhandledQueue;
import uk.nhs.prm.e2etests.pdsadaptor.PdsAdaptorClient;
import uk.nhs.prm.e2etests.queue.ForceXercesParserSoLogbackDoesNotBlowUpWhenUsingSwiftMqClient;
import uk.nhs.prm.e2etests.reregistration.ReRegistrationMessageObservabilityQueue;
import uk.nhs.prm.e2etests.reregistration.models.ActiveSuspensionsMessage;
import uk.nhs.prm.e2etests.services.ehr_repo.EhrRepoClient;
import uk.nhs.prm.e2etests.suspensions.*;
import uk.nhs.prm.e2etests.utility.NemsEventFactory;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.nhs.prm.e2etests.nhs.NhsIdentityGenerator.*;
import static uk.nhs.prm.e2etests.utility.NemsEventFactory.createNemsEventFromTemplate;

@SpringBootTest
@ExtendWith(ForceXercesParserSoLogbackDoesNotBlowUpWhenUsingSwiftMqClient.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ContinuityE2E {
    @Autowired
    private MeshForwarderQueue meshForwarderQueue;
    @Autowired
    private NemsEventProcessorUnhandledQueue nemsEventProcessorUnhandledQueue;
    @Autowired
    private SuspensionMessageObservabilityQueue suspensionsMessageQueue;
    @Autowired
    private SuspensionServiceNotReallySuspensionsMessageQueue notReallySuspensionsMessageQueue;
    @Autowired
    private MofUpdatedMessageQueue mofUpdatedMessageQueue;
    @Autowired
    private MofNotUpdatedMessageQueue mofNotUpdatedMessageQueue;
    @Autowired
    private NemsEventProcessorDeadLetterQueue nemsEventProcessorDeadLetterQueue;
    @Autowired
    private DeceasedPatientQueue deceasedPatientQueue;
    @Autowired
    private ReRegistrationMessageObservabilityQueue reRegistrationMessageObservabilityQueue;
    @Autowired
    private MeshMailbox meshMailbox;
    @Autowired
    private TestConfiguration config;
    @Autowired
    private RepoIncomingObservabilityQueue repoIncomingObservabilityQueue;
    @Autowired
    private ActiveSuspensionsDB activeSuspensionsDB;
    @Autowired
    private EhrRepoClient ehrRepoClient;
    @Autowired
    private PdsAdaptorProperties pdsAdaptorProperties;
    @Autowired
    private NhsProperties nhsProperties;

    private PdsAdaptorClient pdsAdaptorClient;

    private final String EMIS_PTL_INT = "N82668";
    private final String SUSPENDED_PATIENT_NHS_NUMBER = "9693796047";

    @BeforeAll
    void init() {
        meshForwarderQueue.deleteAllMessages();
        nemsEventProcessorDeadLetterQueue.deleteAllMessages();
        suspensionsMessageQueue.deleteAllMessages();
        nemsEventProcessorUnhandledQueue.deleteAllMessages();
        notReallySuspensionsMessageQueue.deleteAllMessages();
        reRegistrationMessageObservabilityQueue.deleteAllMessages();
        repoIncomingObservabilityQueue.deleteAllMessages();
        pdsAdaptorClient = new PdsAdaptorClient(
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
        String suspendedPatientNhsNumber = config.getNhsNumberForSyntheticPatientWithoutGp();
        var now = now();
        String previousGp = generateRandomOdsCode();
        System.out.printf("Generated random ods code for previous gp: %s%n", previousGp);

        NemsEventMessage nemsSuspension = createNemsEventFromTemplate("change-of-gp-suspension.xml", suspendedPatientNhsNumber, nemsMessageId, previousGp, now);
        meshMailbox.postMessage(nemsSuspension);
        MofUpdatedMessageNems expectedMessageOnQueue = new MofUpdatedMessageNems(nemsMessageId, "ACTION:UPDATED_MANAGING_ORGANISATION");

        assertThat(meshForwarderQueue.hasMessage(nemsSuspension.getMessage())); // TODO PRMT-3574 an 'assertThat' without a matching 'isTrue'? I think these instances need a change
        assertThat(mofUpdatedMessageQueue.hasResolutionMessage(expectedMessageOnQueue));
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

        assertThat(repoIncomingObservabilityQueue.getMessageContaining(nemsMessageId));

    }

    @Test
    @Order(2)
    public void shouldMoveSuspensionMessageWherePatientIsNoLongerSuspendedToNotSuspendedQueue() {
        String nemsMessageId = randomNemsMessageId();
        String previousGp = generateRandomOdsCode();
        var now = now();
        String currentlyRegisteredPatientNhsNumber = config.getNhsNumberForSyntheticPatientWithCurrentGp();

        NemsEventMessage nemsSuspension = createNemsEventFromTemplate("change-of-gp-suspension.xml", currentlyRegisteredPatientNhsNumber, nemsMessageId, previousGp, now);

        NoLongerSuspendedMessageNems expectedMessageOnQueue = new NoLongerSuspendedMessageNems(nemsMessageId, "NO_ACTION:NO_LONGER_SUSPENDED_ON_PDS");

        meshMailbox.postMessage(nemsSuspension);
        assertThat(notReallySuspensionsMessageQueue.hasResolutionMessage(expectedMessageOnQueue));

    }

    @Test
    @Order(5)
    public void shouldMoveNonSuspensionMessageFromNemsToUnhandledQueue() throws Exception {
        var nemsMessageId = randomNemsMessageId();

        var nemsNonSuspension = createNemsEventFromTemplate(
                "change-of-gp-non-suspension.xml", randomNhsNumber(), nemsMessageId, now());

        meshMailbox.postMessage(nemsNonSuspension);

        assertThat(nemsEventProcessorUnhandledQueue.hasMessage("{\"nemsMessageId\":\"" + nemsMessageId + "\",\"messageStatus\":\"NO_ACTION:NON_SUSPENSION\"}"));
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
                config.getNhsNumberForNonSyntheticPatientWithoutGp(),
                nemsMessageId, previousGp, suspensionTime);

        meshMailbox.postMessage(nemsSuspension);

        NemsResolutionMessage expectedMessageOnQueue = new NemsResolutionMessage(nemsMessageId, "NO_ACTION:NOT_SYNTHETIC_OR_SAFE_LISTED");

        assertThat(mofNotUpdatedMessageQueue.hasResolutionMessage(expectedMessageOnQueue));
    }

    @Test
    @Order(3)
    public void shouldMoveDeceasedPatientEventToDeceasedQueue() {
        String nemsMessageId = randomNemsMessageId();
        String patientNhsNumber = config.getNhsNumberForSyntheticDeceasedPatient();
        var eventTime = now();
        String previousGp = generateRandomOdsCode();

        System.out.printf("Generated random ods code for previous gp: %s%n", previousGp);

        var deceasedEvent = createNemsEventFromTemplate("change-of-gp-suspension.xml",
                patientNhsNumber, nemsMessageId, previousGp, eventTime);

        meshMailbox.postMessage(deceasedEvent);

        var expectedMessageOnQueue = new DeceasedPatientMessageNems(nemsMessageId, "NO_ACTION:DECEASED_PATIENT");

        assertThat(deceasedPatientQueue.hasResolutionMessage(expectedMessageOnQueue));
    }

    @Test
    @Order(7)
    public void shouldDeleteEhrOfPatientOnTheirReRegistration() throws Exception {
        var nemsMessageId = randomNemsMessageId();
        String patientNhsNumber = config.getNhsNumberForSyntheticPatientWithCurrentGp();
        var reregistrationTime = now();
        storeEhrInRepositoryFor(patientNhsNumber);
        activeSuspensionsDB.save(new ActiveSuspensionsMessage(patientNhsNumber,generateRandomOdsCode(), now()));

        var reRegistration = createNemsEventFromTemplate(
                "change-of-gp-re-registration.xml", patientNhsNumber, nemsMessageId, reregistrationTime);

        meshMailbox.postMessage(reRegistration);

        String expectedMessageOnQueue = "{\"nhsNumber\":\"" + patientNhsNumber + "\"," +
                "\"newlyRegisteredOdsCode\":\"B86056\"," +
                "\"nemsMessageId\":\"" + nemsMessageId + "\"," +
                "\"lastUpdated\":\"" + reregistrationTime + "\"}";




        assertThrows(HttpClientErrorException.class, () -> {
            ehrRepoClient.getEhrResponse(patientNhsNumber);
        });


        assertThat(meshForwarderQueue.hasMessage(reRegistration.getMessage()));
        assertThat(reRegistrationMessageObservabilityQueue.hasMessage(expectedMessageOnQueue));
    }

    @Test
    @DisabledIfEnvironmentVariable(named = "UPDATE_MOF_TO_REPO",matches="true")
    void shouldSaveActiveSuspensionInDbWhenMofUpdatedToPreviousGp() {
        String nemsMessageId = randomNemsMessageId();
        String suspendedPatientNhsNumber = config.getNhsNumberForSyntheticPatientWithoutGp();
        var now = now();
        String previousGp = generateRandomOdsCode();
        System.out.printf("Generated random ods code for previous gp: %s%n", previousGp);

        NemsEventMessage nemsSuspension = createNemsEventFromTemplate("change-of-gp-suspension.xml", suspendedPatientNhsNumber, nemsMessageId, previousGp, now);
        meshMailbox.postMessage(nemsSuspension);

        assertTrue(activeSuspensionsDB.nhsNumberExists(suspendedPatientNhsNumber));
    }


    private static String now() {
        return ZonedDateTime.now(ZoneOffset.ofHours(0)).toString();
    }

    private void storeEhrInRepositoryFor(String patientNhsNumber) throws Exception {
        ehrRepoClient.createEhr(patientNhsNumber);
        assertThat(ehrRepoClient.getEhrResponse(patientNhsNumber)).isEqualTo("200 OK");
    }

    private void setManagingOrganisationToEMISOdsCode(String nhsNumber) {
        var pdsResponse = pdsAdaptorClient.getSuspendedPatientStatus(nhsNumber);
        var repoOdsCode = Gp2GpSystem.repoInEnv(nhsProperties.getNhsEnvironment()).odsCode();
        if (repoOdsCode.equals(pdsResponse.getManagingOrganisation())) {
            pdsAdaptorClient.updateManagingOrganisation(nhsNumber, EMIS_PTL_INT, pdsResponse.getRecordETag());
        }
    }

    public void log(String message) {
        System.out.println(message);
    }
}
