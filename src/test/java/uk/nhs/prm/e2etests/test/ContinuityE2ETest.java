package uk.nhs.prm.e2etests.test;

import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.HttpClientErrorException;
import uk.nhs.prm.e2etests.enumeration.Gp2GpSystem;
import uk.nhs.prm.e2etests.enumeration.TemplateVariant;
import uk.nhs.prm.e2etests.mesh.MeshMailbox;
import uk.nhs.prm.e2etests.model.ActiveSuspensionsMessage;
import uk.nhs.prm.e2etests.model.nems.*;
import uk.nhs.prm.e2etests.model.response.PdsAdaptorResponse;
import uk.nhs.prm.e2etests.model.templatecontext.NemsEventTemplateContext;
import uk.nhs.prm.e2etests.property.NhsProperties;
import uk.nhs.prm.e2etests.property.SyntheticPatientProperties;
import uk.nhs.prm.e2etests.queue.nems.NemsEventProcessorDeadLetterQueue;
import uk.nhs.prm.e2etests.queue.nems.NemsEventProcessorUnhandledEventsQueue;
import uk.nhs.prm.e2etests.queue.nems.observability.MeshForwarderOQ;
import uk.nhs.prm.e2etests.queue.nems.observability.NemsEventProcessorReRegistrationsOQ;
import uk.nhs.prm.e2etests.queue.nems.observability.NemsEventProcessorSuspensionsOQ;
import uk.nhs.prm.e2etests.queue.suspensions.SuspensionServiceDeceasedPatientQueue;
import uk.nhs.prm.e2etests.queue.suspensions.SuspensionServiceMofNotUpdatedQueue;
import uk.nhs.prm.e2etests.queue.suspensions.SuspensionServiceMofUpdatedQueue;
import uk.nhs.prm.e2etests.queue.suspensions.observability.SuspensionServiceNotSuspendedOQ;
import uk.nhs.prm.e2etests.queue.suspensions.observability.SuspensionsServiceRepoIncomingOQ;
import uk.nhs.prm.e2etests.service.ActiveSuspensionsService;
import uk.nhs.prm.e2etests.service.EhrRepositoryService;
import uk.nhs.prm.e2etests.service.PdsAdaptorService;
import uk.nhs.prm.e2etests.service.TemplatingService;
import uk.nhs.prm.e2etests.utility.NemsEventGenerator;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.nhs.prm.e2etests.utility.NhsIdentityUtility.randomNemsMessageId;
import static uk.nhs.prm.e2etests.utility.NhsIdentityUtility.randomNhsNumber;
import static uk.nhs.prm.e2etests.utility.NhsIdentityUtility.randomOdsCode;

@Log4j2
@SpringBootTest
@ExtendWith(ForceXercesParserSoLogbackDoesNotBlowUpWhenUsingSwiftMqClient.class)
@TestPropertySource(properties = {"test.pds.username=e2e-test"})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ContinuityE2ETest {
    private static final String EMIS_PTL_INT = "N82668";
    private static final String SUSPENDED_PATIENT_NHS_NUMBER = "9693796047";

    private final MeshMailbox meshMailbox;
    private final PdsAdaptorService pdsAdaptorService;
    private final EhrRepositoryService ehrRepositoryService;
    private final ActiveSuspensionsService activeSuspensionsService;
    private final NemsEventProcessorUnhandledEventsQueue nemsEventProcessorUnhandledEventsQueue;
    private final NemsEventProcessorSuspensionsOQ nemsEventProcessorSuspensionsOQ;
    private final NemsEventProcessorDeadLetterQueue nemsEventProcessorDeadLetterQueue;
    private final NemsEventProcessorReRegistrationsOQ nemsEventProcessorReRegistrationsOQ;
    private final SuspensionServiceNotSuspendedOQ suspensionServiceNotSuspendedOQ;
    private final SuspensionServiceMofUpdatedQueue suspensionServiceMofUpdatedQueue;
    private final SuspensionServiceMofNotUpdatedQueue suspensionServiceMofNotUpdatedQueue;
    private final SuspensionServiceDeceasedPatientQueue suspensionServiceDeceasedPatientQueue;
    private final SuspensionsServiceRepoIncomingOQ suspensionsServiceRepoIncomingOQ;
    private final TemplatingService templatingService;
    private final MeshForwarderOQ meshForwarderOQ;
    private final SyntheticPatientProperties syntheticPatientProperties;
    private final NhsProperties nhsProperties;

    @Autowired
    public ContinuityE2ETest(
            MeshMailbox meshMailbox,
            PdsAdaptorService pdsAdaptorService,
            EhrRepositoryService ehrRepositoryService,
            ActiveSuspensionsService activeSuspensionsService,
            NemsEventProcessorUnhandledEventsQueue nemsEventProcessorUnhandledEventsQueue,
            NemsEventProcessorSuspensionsOQ nemsEventProcessorSuspensionsOQ,
            NemsEventProcessorDeadLetterQueue nemsEventProcessorDeadLetterQueue,
            NemsEventProcessorReRegistrationsOQ nemsEventProcessorReRegistrationsOQ,
            SuspensionServiceNotSuspendedOQ suspensionServiceNotSuspendedOQ,
            SuspensionServiceMofUpdatedQueue suspensionServiceMofUpdatedQueue,
            SuspensionServiceMofNotUpdatedQueue suspensionServiceMofNotUpdatedQueue,
            SuspensionServiceDeceasedPatientQueue suspensionServiceDeceasedPatientQueue,
            SuspensionsServiceRepoIncomingOQ suspensionsServiceRepoIncomingOQ,
            TemplatingService templatingService,
            MeshForwarderOQ meshForwarderOQ,
            SyntheticPatientProperties syntheticPatientProperties,
            NhsProperties nhsProperties
    ) {
        this.meshMailbox = meshMailbox;
        this.pdsAdaptorService = pdsAdaptorService;
        this.ehrRepositoryService = ehrRepositoryService;
        this.activeSuspensionsService = activeSuspensionsService;
        this.nemsEventProcessorUnhandledEventsQueue = nemsEventProcessorUnhandledEventsQueue;
        this.nemsEventProcessorSuspensionsOQ = nemsEventProcessorSuspensionsOQ;
        this.nemsEventProcessorDeadLetterQueue = nemsEventProcessorDeadLetterQueue;
        this.nemsEventProcessorReRegistrationsOQ = nemsEventProcessorReRegistrationsOQ;
        this.suspensionServiceNotSuspendedOQ = suspensionServiceNotSuspendedOQ;
        this.suspensionServiceMofUpdatedQueue = suspensionServiceMofUpdatedQueue;
        this.suspensionServiceMofNotUpdatedQueue = suspensionServiceMofNotUpdatedQueue;
        this.suspensionServiceDeceasedPatientQueue = suspensionServiceDeceasedPatientQueue;
        this.suspensionsServiceRepoIncomingOQ = suspensionsServiceRepoIncomingOQ;
        this.templatingService = templatingService;
        this.meshForwarderOQ =  meshForwarderOQ;
        this.syntheticPatientProperties = syntheticPatientProperties;
        this.nhsProperties = nhsProperties;
    }

    @BeforeAll
    void init() {
        meshForwarderOQ.deleteAllMessages();
        nemsEventProcessorDeadLetterQueue.deleteAllMessages();
        nemsEventProcessorSuspensionsOQ.deleteAllMessages();
        nemsEventProcessorUnhandledEventsQueue.deleteAllMessages();
        suspensionServiceMofUpdatedQueue.deleteAllMessages();
        suspensionServiceNotSuspendedOQ.deleteAllMessages();
        nemsEventProcessorReRegistrationsOQ.deleteAllMessages();
        suspensionsServiceRepoIncomingOQ.deleteAllMessages();
    }

    @Test
    @DisabledIfEnvironmentVariable(named = "UPDATE_MOF_TO_REPO", matches="true")
    @Order(1)
    void shouldMoveSuspensionMessageFromNemsToMofUpdatedQueue() {
        String nemsMessageId = randomNemsMessageId();
        String suspendedPatientNhsNumber = syntheticPatientProperties.getPatientWithoutGp();
        String now = now();
        String previousGp = randomOdsCode();
        log.info("Generated a random ODS code for previous GP: {}", previousGp);

        NemsEventMessage nemsSuspension = templatingService.createNemsEventFromTemplate(
                TemplateVariant.CHANGE_OF_GP_NON_SUSPENSION,
                suspendedPatientNhsNumber,
                nemsMessageId,
                previousGp,
                now
        );

        meshMailbox.sendMessage(nemsSuspension);
        NemsResolutionMessage expectedMessageOnQueue = new MofUpdatedMessageNems(nemsMessageId, "ACTION:UPDATED_MANAGING_ORGANISATION");

        assertTrue(meshForwarderOQ.hasMessage(nemsSuspension.getMessage()));
        assertTrue(suspensionServiceMofUpdatedQueue.hasResolutionMessage(expectedMessageOnQueue));
    }

    @Test
    @DisabledIfEnvironmentVariable(named = "UPDATE_MOF_TO_REPO",matches="false") //The toggle status for repo_process_only_safe_listed_ods_codes is true in dev and test
    void shouldPutAMessageForASuspendedPatientWithSafeListedODSCodeOnRepoIncomingWhenTheToggleIsTrue() {
        String nemsMessageId = randomNemsMessageId();
        String now = now();
        NemsEventMessage nemsSuspension = templatingService.createNemsEventFromTemplate(
                TemplateVariant.CHANGE_OF_GP_SUSPENSION,
                SUSPENDED_PATIENT_NHS_NUMBER,
                nemsMessageId, EMIS_PTL_INT,
                now
        );

        setManagingOrganisationToEMISOdsCode(SUSPENDED_PATIENT_NHS_NUMBER);

        meshMailbox.sendMessage(nemsSuspension);

        assertThat(suspensionsServiceRepoIncomingOQ.getMessageContaining(nemsMessageId)).isNotNull();
    }

    @Test
    @Order(2)
    void shouldMoveSuspensionMessageWherePatientIsNoLongerSuspendedToNotSuspendedQueue() {
        String nemsMessageId = randomNemsMessageId();
        String previousGp = randomOdsCode();
        String now = now();
        String currentlyRegisteredPatientNhsNumber = syntheticPatientProperties.getPatientWithCurrentGp();

        NemsEventMessage nemsSuspension = templatingService.createNemsEventFromTemplate(
                TemplateVariant.CHANGE_OF_GP_SUSPENSION,currentlyRegisteredPatientNhsNumber, nemsMessageId, previousGp, now
        );


        NoLongerSuspendedMessageNems expectedMessageOnQueue = new NoLongerSuspendedMessageNems(nemsMessageId, "NO_ACTION:NO_LONGER_SUSPENDED_ON_PDS");

        meshMailbox.sendMessage(nemsSuspension);
        assertTrue(suspensionServiceNotSuspendedOQ.hasResolutionMessage(expectedMessageOnQueue));
    }

    @Test
    @Order(5)
    void shouldMoveNonSuspensionMessageFromNemsToUnhandledQueue() {
        String nemsMessageId = randomNemsMessageId();

        NemsEventMessage nemsNonSuspension = templatingService.createNemsEventFromTemplate(
                TemplateVariant.CHANGE_OF_GP_NON_SUSPENSION,
                randomNhsNumber(),
                nemsMessageId,
                "B85612", // this previous GP was hardcoded in previous implementation
                now()
        );

        meshMailbox.sendMessage(nemsNonSuspension);

        assertTrue(nemsEventProcessorUnhandledEventsQueue.hasMessage("{\"nemsMessageId\":\"" + nemsMessageId + "\",\"messageStatus\":\"NO_ACTION:NON_SUSPENSION\"}"));
    }

    @Test
    @Order(6)
    void shouldSendUnprocessableMessagesToDlQ() {
        Map<String, NemsEventMessage> dlqMessages = templatingService.getDLQNemsEventMessages();
        log.info("Posting DLQ messages.");

        for (Map.Entry<String, NemsEventMessage> message : dlqMessages.entrySet()) {
            meshMailbox.sendMessage(message.getValue());
            log.info("Posted {} messages.", message.getKey());
            assertTrue(nemsEventProcessorDeadLetterQueue.hasMessage(message.getValue().getMessage()));
        }
    }

    @Test
    @Order(4)
    @Disabled(" 'process_only_synthetic_or_safe_listed_patients' toggle is set to false across all the environments. ")
    void shouldMoveNonSyntheticPatientSuspensionMessageFromNemsToMofNotUpdatedQueueWhenToggleOn() {
        String nemsMessageId = randomNemsMessageId();
        String previousGp = randomOdsCode();
        String suspensionTime = now();

        NemsEventMessage nemsSuspension = templatingService.createNemsEventFromTemplate(
                TemplateVariant.CHANGE_OF_GP_SUSPENSION,
                syntheticPatientProperties.getNonSyntheticPatientWithoutGp(),
                nemsMessageId,
                previousGp,
                suspensionTime
        );

        meshMailbox.sendMessage(nemsSuspension);

        NemsResolutionMessage expectedMessageOnQueue = new NemsResolutionMessage(nemsMessageId, "NO_ACTION:NOT_SYNTHETIC_OR_SAFE_LISTED");

        assertTrue(suspensionServiceMofNotUpdatedQueue.hasResolutionMessage(expectedMessageOnQueue));
    }

    @Test
    @Order(3)
    void shouldMoveDeceasedPatientEventToDeceasedQueue() {
        String nemsMessageId = randomNemsMessageId();
        String patientNhsNumber = syntheticPatientProperties.getDeceasedPatient();
        String eventTime = now();
        String previousGp = randomOdsCode();

        log.info("Generated a random ODS code for previous GP: {}.", previousGp);

        NemsEventMessage deceasedEvent = templatingService.createNemsEventFromTemplate(
                TemplateVariant.CHANGE_OF_GP_SUSPENSION,
                patientNhsNumber, nemsMessageId, previousGp, eventTime
        );

        meshMailbox.sendMessage(deceasedEvent);

        DeceasedPatientMessageNems expectedMessageOnQueue = new DeceasedPatientMessageNems(nemsMessageId, "NO_ACTION:DECEASED_PATIENT");

        assertTrue(suspensionServiceDeceasedPatientQueue.hasResolutionMessage(expectedMessageOnQueue));
    }

    @Test
    @Order(7)
    void shouldDeleteEhrOfPatientOnTheirReRegistration() throws Exception {
        String nemsMessageId = randomNemsMessageId();
        String patientNhsNumber = syntheticPatientProperties.getPatientWithCurrentGp();
        String reregistrationTime = now();
        storeEhrInRepositoryFor(patientNhsNumber);
        activeSuspensionsService.save(new ActiveSuspensionsMessage(patientNhsNumber, randomOdsCode(), now()));

        NemsEventMessage reRegistration = templatingService.createNemsEventFromTemplate(
                TemplateVariant.CHANGE_OF_GP_RE_REGISTRATION,
                patientNhsNumber,
                nemsMessageId,
                "B85612", // this previous GP was hardcoded in previous implementation
                reregistrationTime
        );

        meshMailbox.sendMessage(reRegistration);

        String expectedMessageOnQueue = "{\"nhsNumber\":\"" + patientNhsNumber + "\"," +
                "\"newlyRegisteredOdsCode\":\"B86056\"," +
                "\"nemsMessageId\":\"" + nemsMessageId + "\"," +
                "\"lastUpdated\":\"" + reregistrationTime + "\"}";

        assertThrows(HttpClientErrorException.class, () -> ehrRepositoryService.getEhrResponse(patientNhsNumber));

        assertTrue(meshForwarderOQ.hasMessage(reRegistration.getMessage()));
        assertTrue(nemsEventProcessorReRegistrationsOQ.hasMessage(expectedMessageOnQueue));
    }

    @Test
    @DisabledIfEnvironmentVariable(named = "UPDATE_MOF_TO_REPO",matches="true")
    void shouldSaveActiveSuspensionInDbWhenMofUpdatedToPreviousGp() {
        String nemsMessageId = randomNemsMessageId();
        String suspendedPatientNhsNumber = syntheticPatientProperties.getPatientWithoutGp();
        String now = now();
        String previousGp = randomOdsCode();
        log.info("Generated a random ODS code for previous GP: {}.", previousGp);

        NemsEventMessage nemsSuspension = templatingService.createNemsEventFromTemplate(
                TemplateVariant.CHANGE_OF_GP_SUSPENSION,
                suspendedPatientNhsNumber,
                nemsMessageId,
                previousGp,
                now
        );
        meshMailbox.sendMessage(nemsSuspension);

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
        PdsAdaptorResponse pdsResponse = pdsAdaptorService.getSuspendedPatientStatus(nhsNumber);
        String repoOdsCode = Gp2GpSystem.repoInEnv(nhsProperties.getNhsEnvironment()).odsCode();
        if (repoOdsCode.equals(pdsResponse.managingOrganisation())) {
            pdsAdaptorService.updateManagingOrganisation(nhsNumber, EMIS_PTL_INT, pdsResponse.recordETag());
        }
    }
}
