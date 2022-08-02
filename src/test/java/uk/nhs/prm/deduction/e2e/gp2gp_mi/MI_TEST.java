package uk.nhs.prm.deduction.e2e.gp2gp_mi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.nhs.prm.deduction.e2e.gp2gp_mi.ehr_generate_event.*;
import uk.nhs.prm.deduction.e2e.gp2gp_mi.ehr_generate_event.Attachment;
import uk.nhs.prm.deduction.e2e.gp2gp_mi.ehr_generate_event.Ehr;
import uk.nhs.prm.deduction.e2e.gp2gp_mi.ehr_generate_event.Placeholder;
import uk.nhs.prm.deduction.e2e.gp2gp_mi.ehr_requested_event.EhrRequestedEventRequest;
import uk.nhs.prm.deduction.e2e.gp2gp_mi.ehr_requested_event.PayloadEhrRequested;
import uk.nhs.prm.deduction.e2e.gp2gp_mi.ehr_sent_event.EhrSentEventRequest;
import uk.nhs.prm.deduction.e2e.gp2gp_mi.ehr_sent_event.PayloadEhrSent;
import uk.nhs.prm.deduction.e2e.gp2gp_mi.ehr_validated_event.*;
import uk.nhs.prm.deduction.e2e.gp2gp_mi.error_event.Error;
import uk.nhs.prm.deduction.e2e.gp2gp_mi.error_event.ErrorEventRequest;
import uk.nhs.prm.deduction.e2e.gp2gp_mi.error_event.PayloadErrorEvent;
import uk.nhs.prm.deduction.e2e.gp2gp_mi.integrated_event.EhrIntegratedEventRequest;
import uk.nhs.prm.deduction.e2e.gp2gp_mi.integrated_event.Integration;
import uk.nhs.prm.deduction.e2e.gp2gp_mi.integrated_event.PayloadIntegratedEvent;
import uk.nhs.prm.deduction.e2e.gp2gp_mi.migrate_structured_record_request.MigrateStructuredRecordEventRequest;
import uk.nhs.prm.deduction.e2e.gp2gp_mi.migrate_structured_record_request.PayloadMigrateStructuredRecordEvent;
import uk.nhs.prm.deduction.e2e.gp2gp_mi.pds_general_update.DemographicTraceStatusGeneralUpdate;
import uk.nhs.prm.deduction.e2e.gp2gp_mi.pds_general_update.PDSGeneralUpdateEventRequest;
import uk.nhs.prm.deduction.e2e.gp2gp_mi.pds_general_update.PdsGeneralUpdate;
import uk.nhs.prm.deduction.e2e.gp2gp_mi.pds_trace.DemographicTraceStatus;
import uk.nhs.prm.deduction.e2e.gp2gp_mi.pds_trace.PDSTraceEventRequest;
import uk.nhs.prm.deduction.e2e.gp2gp_mi.pds_trace.PayloadTrace;
import uk.nhs.prm.deduction.e2e.gp2gp_mi.registration_started.MiResponse;
import uk.nhs.prm.deduction.e2e.gp2gp_mi.registration_started.Payload;
import uk.nhs.prm.deduction.e2e.gp2gp_mi.registration_started.RegistrationStartedRequest;
import uk.nhs.prm.deduction.e2e.gp2gp_mi.sds_lookup.PayloadSDSLookUp;
import uk.nhs.prm.deduction.e2e.gp2gp_mi.sds_lookup.SDSLookUpEventRequest;
import uk.nhs.prm.deduction.e2e.gp2gp_mi.sds_lookup.TransferCompatibilityStatus;

import static org.junit.jupiter.api.Assertions.assertNotNull;


@SpringBootTest(classes = {
        MI_TEST.class,
        Gp2GpMIClient.class

})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class MI_TEST {
    @Autowired
    Gp2GpMIClient gp2GpMIClient;

    @Test
    void shouldReturnEventIdSubmitRegistrationStartedEvent() throws JsonProcessingException {
        RegistrationStartedRequest request = new RegistrationStartedRequest(new Payload(new uk.nhs.prm.deduction.e2e.gp2gp_mi.registration_started.Registration("newRegistrant", "ABC1234")));
        String updateManagingOrganisation = gp2GpMIClient.update(request, "preTransfer/registrationStarted");
        var registrationStartedResponse = new ObjectMapper().readValue(updateManagingOrganisation, MiResponse.class);
        assertNotNull(registrationStartedResponse.getEventId());
    }

    @Test
    void shouldReturnEventIdSubmitPDSTraceEvent() throws JsonProcessingException {
        PDSTraceEventRequest request = new PDSTraceEventRequest(new PayloadTrace(true, new DemographicTraceStatus("FAILURE", "patient not found")));
        String updateManagingOrganisation = gp2GpMIClient.update(request, "preTransfer/pdsTrace");
        var registrationStartedResponse = new ObjectMapper().readValue(updateManagingOrganisation, MiResponse.class);
        assertNotNull(registrationStartedResponse.getEventId());
    }

    @Test
    void shouldReturnEventIdSubmitSDSLookUpEvent() throws JsonProcessingException {
        SDSLookUpEventRequest request = new SDSLookUpEventRequest(new PayloadSDSLookUp(new TransferCompatibilityStatus("FAILURE")));
        String updateManagingOrganisation = gp2GpMIClient.update(request, "preTransfer/sdsLookup");
        var registrationStartedResponse = new ObjectMapper().readValue(updateManagingOrganisation, MiResponse.class);
        assertNotNull(registrationStartedResponse.getEventId());
    }

    @Test
    void shouldReturnEventIdPdsGeneralUpdateEvent() throws JsonProcessingException {
        PDSGeneralUpdateEventRequest request = new PDSGeneralUpdateEventRequest(new PdsGeneralUpdate(new DemographicTraceStatusGeneralUpdate("FAILURE")));
        String updateManagingOrganisation = gp2GpMIClient.update(request, "preTransfer/pdsGeneralUpdate");
        var registrationStartedResponse = new ObjectMapper().readValue(updateManagingOrganisation, MiResponse.class);
        assertNotNull(registrationStartedResponse.getEventId());
    }

    @Test
    void shouldReturnEventIdSubmitEhrRequestedEvent() throws JsonProcessingException {
        EhrRequestedEventRequest request = new EhrRequestedEventRequest(new PayloadEhrRequested(new Registration("ABC1234", "XYZ4567")));
        String ehrRequestedEvent = gp2GpMIClient.update(request, "gp2gp/ehrRequested");
        var registrationStartedResponse = new ObjectMapper().readValue(ehrRequestedEvent, MiResponse.class);
        assertNotNull(registrationStartedResponse.getEventId());
    }

    @Test
    void shouldReturnEventIdSubmitEhrSentEvent() throws JsonProcessingException {
        EhrSentEventRequest request = new EhrSentEventRequest(new PayloadEhrSent(new Registration("ABC1234", "XYZ4567")));
        String ehrRequestedEvent = gp2GpMIClient.update(request, "gp2gp/ehrSent");
        var registrationStartedResponse = new ObjectMapper().readValue(ehrRequestedEvent, MiResponse.class);
        assertNotNull(registrationStartedResponse.getEventId());
    }

    @Test
    void shouldReturnEventIdSubmitErrorEvent() throws JsonProcessingException {
        ErrorEventRequest request = new ErrorEventRequest(new PayloadErrorEvent(new Registration("ABC1234", "XYZ4567"), new Error("ABC", "stacktrace? detailed error message?")));
        String ehrRequestedEvent = gp2GpMIClient.update(request, "gp2gp/error");
        var registrationStartedResponse = new ObjectMapper().readValue(ehrRequestedEvent, MiResponse.class);
        assertNotNull(registrationStartedResponse.getEventId());
    }

    @Test
    void shouldReturnEventIdSubmitErrorEventForGpConnect() throws JsonProcessingException {
        ErrorEventRequest request = new ErrorEventRequest(new PayloadErrorEvent(new Registration("ABC1234", "XYZ4567"), new Error("ABC", "stacktrace? detailed error message?")));
        String ehrRequestedEvent = gp2GpMIClient.update(request, "gpconnect/error");
        var registrationStartedResponse = new ObjectMapper().readValue(ehrRequestedEvent, MiResponse.class);
        assertNotNull(registrationStartedResponse.getEventId());
    }

    @Test
    void shouldReturnEventIdForEhrIntegratedEvent() throws JsonProcessingException {
        EhrIntegratedEventRequest request = new EhrIntegratedEventRequest(
                new PayloadIntegratedEvent(
                        new Registration("ABC1234", "XYZ4567"),
                        new Integration("filed as attachment/ suppressed/ merged/ rejected", "Reason for integration status")));
        String ehrIntegratedEvent = gp2GpMIClient.update(request, "gp2gp/ehrIntegrated");
        var registrationStartedResponse = new ObjectMapper().readValue(ehrIntegratedEvent, MiResponse.class);
        assertNotNull(registrationStartedResponse.getEventId());
    }

    @Test
    void shouldReturnEventIdForEhrGeneratedEvent() throws JsonProcessingException {
        EhrGeneratedEventRequest request = new EhrGeneratedEventRequest(
                new PayloadEhrGeneratedEvent(
                        new Registration("ABC1234", "XYZ4567"),
                        new Ehr(5699433L, 4096L,
                                  new Placeholder[]{new Placeholder("9876-987654-9876-987654", "1323-132345-1323-132345", "XYZ4567", 1, "audio/mpeg")},
                                  new Attachment[]{new Attachment("3424-342456-3424-342456", "Scanned document", "application/pdf", 3084322L)}),
                        new UnsupportedDataItem[]{new UnsupportedDataItem("allergy/flag", "1323-132345-1323-132345", "reason for being unsupported / why is it unsupported in gp2gp / what would have to change in gp2gp to express this")}));
        String ehrGeneratedEvent = gp2GpMIClient.update(request, "gp2gp/ehrGenerated");
        var registrationStartedResponse = new ObjectMapper().readValue(ehrGeneratedEvent, MiResponse.class);
        assertNotNull(registrationStartedResponse.getEventId());
    }

    @Test
    void shouldReturnEventIdForEhrValidatedEvent() throws JsonProcessingException {
        EhrValidatedEventRequest request = new EhrValidatedEventRequest(
                new PayloadEhrValidatedEvent(
                        new Registration("ABC1234", "XYZ4567"),
                        new  uk.nhs.prm.deduction.e2e.gp2gp_mi.ehr_validated_event.Ehr(5699433L, 4096L,
                                new uk.nhs.prm.deduction.e2e.gp2gp_mi.ehr_validated_event.Placeholder[]{new uk.nhs.prm.deduction.e2e.gp2gp_mi.ehr_validated_event.Placeholder("9876-987654-9876-987654", "1323-132345-1323-132345", "XYZ4567", 1, "audio/mpeg")},
                                new uk.nhs.prm.deduction.e2e.gp2gp_mi.ehr_validated_event.Attachment[]{new uk.nhs.prm.deduction.e2e.gp2gp_mi.ehr_validated_event.Attachment("3424-342456-3424-342456", "Scanned document", "application/pdf", 3084322L)},
                                new Degrade[]{new Degrade("core_ehr / attachment", "other degrades metadata", new Code(new Coding[]{new Coding("886721000000107", "http://snomed.info/sct")}))})));
        String ehrValidatedEvent = gp2GpMIClient.update(request, "gp2gp/ehrValidated");
        var registrationStartedResponse = new ObjectMapper().readValue(ehrValidatedEvent, MiResponse.class);
        assertNotNull(registrationStartedResponse.getEventId());
    }

    @Test
    void shouldReturnEventIdForEhrIntegratedEventGpConnect() throws JsonProcessingException {
        EhrIntegratedEventRequest request = new EhrIntegratedEventRequest(
                new PayloadIntegratedEvent(
                        new Registration("ABC1234", "XYZ4567"),
                        new Integration("filed as attachment/ suppressed/ merged/ rejected", "Reason for integration status")));
        String ehrIntegratedEvent = gp2GpMIClient.update(request, "gpconnect/ehrIntegrated");
        var registrationStartedResponse = new ObjectMapper().readValue(ehrIntegratedEvent, MiResponse.class);
        assertNotNull(registrationStartedResponse.getEventId());
    }

    @Test
    void shouldReturnEventIdMigrateStructuredRecordRequestEventGpConnect() throws JsonProcessingException {
        MigrateStructuredRecordEventRequest request = new MigrateStructuredRecordEventRequest(new PayloadMigrateStructuredRecordEvent(new Registration("ABC1234", "XYZ4567")));
        String migrateStructuredRecord = gp2GpMIClient.update(request, "gpconnect/migrateStructuredRecordRequest");
        var registrationStartedResponse = new ObjectMapper().readValue(migrateStructuredRecord, MiResponse.class);
        assertNotNull(registrationStartedResponse.getEventId());
    }
}
