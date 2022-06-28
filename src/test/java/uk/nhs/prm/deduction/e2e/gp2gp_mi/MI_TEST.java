package uk.nhs.prm.deduction.e2e.gp2gp_mi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.nhs.prm.deduction.e2e.gp2gp_mi.pds_general_update.DemographicTraceStatusGeneralUpdate;
import uk.nhs.prm.deduction.e2e.gp2gp_mi.pds_general_update.PDSGeneralUpdateEventRequest;
import uk.nhs.prm.deduction.e2e.gp2gp_mi.pds_general_update.PdsGeneralUpdate;
import uk.nhs.prm.deduction.e2e.gp2gp_mi.pds_trace.DemographicTraceStatus;
import uk.nhs.prm.deduction.e2e.gp2gp_mi.pds_trace.PDSTraceEventRequest;
import uk.nhs.prm.deduction.e2e.gp2gp_mi.pds_trace.PayloadTrace;
import uk.nhs.prm.deduction.e2e.gp2gp_mi.registration_started.Payload;
import uk.nhs.prm.deduction.e2e.gp2gp_mi.registration_started.Registration;
import uk.nhs.prm.deduction.e2e.gp2gp_mi.registration_started.RegistrationStartedRequest;
import uk.nhs.prm.deduction.e2e.gp2gp_mi.registration_started.RegistrationStartedResponse;
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
        RegistrationStartedRequest request = new RegistrationStartedRequest(new Payload(new Registration("newRegistrant", "ABC1234")));
        String updateManagingOrganisation = gp2GpMIClient.updateManagingOrganisation(request,"preTransfer/registrationStarted");
        var registrationStartedResponse = new ObjectMapper().readValue(updateManagingOrganisation, RegistrationStartedResponse.class);
        assertNotNull(registrationStartedResponse.getEventId()) ;
    }
    @Test
    void shouldReturnEventIdSubmitPDSTraceEvent() throws JsonProcessingException {
        PDSTraceEventRequest request = new PDSTraceEventRequest(new PayloadTrace(true, new DemographicTraceStatus("FAILURE", "patient not found")));
        String updateManagingOrganisation = gp2GpMIClient.updateManagingOrganisation(request,"preTransfer/pdsTrace");
        var registrationStartedResponse = new ObjectMapper().readValue(updateManagingOrganisation, RegistrationStartedResponse.class);
        assertNotNull(registrationStartedResponse.getEventId()) ;
    }
    @Test
    void shouldReturnEventIdSubmitSDSLookUpEvent() throws JsonProcessingException {
        SDSLookUpEventRequest request = new SDSLookUpEventRequest(new PayloadSDSLookUp(new TransferCompatibilityStatus("FAILURE")));
        String updateManagingOrganisation = gp2GpMIClient.updateManagingOrganisation(request,"preTransfer/sdsLookup");
        var registrationStartedResponse = new ObjectMapper().readValue(updateManagingOrganisation, RegistrationStartedResponse.class);
        assertNotNull(registrationStartedResponse.getEventId()) ;
    }
    @Test
    void shouldReturnEventIdPdsGeneralUpdateEvent() throws JsonProcessingException {
        PDSGeneralUpdateEventRequest request = new PDSGeneralUpdateEventRequest(new PdsGeneralUpdate(new DemographicTraceStatusGeneralUpdate("FAILURE")));
        String updateManagingOrganisation = gp2GpMIClient.updateManagingOrganisation(request,"preTransfer/pdsGeneralUpdate");
        var registrationStartedResponse = new ObjectMapper().readValue(updateManagingOrganisation, RegistrationStartedResponse.class);
        assertNotNull(registrationStartedResponse.getEventId()) ;
    }

    @Test
    void shouldReturnEventIdSubmitEhrRequestedEvent() throws JsonProcessingException {
        PDSGeneralUpdateEventRequest request = new PDSGeneralUpdateEventRequest(new PdsGeneralUpdate(new DemographicTraceStatusGeneralUpdate("FAILURE")));
        String updateManagingOrganisation = gp2GpMIClient.updateManagingOrganisation(request,"gp2gp/ehrRequested");
        var registrationStartedResponse = new ObjectMapper().readValue(updateManagingOrganisation, RegistrationStartedResponse.class);
        assertNotNull(registrationStartedResponse.getEventId()) ;
    }
}
