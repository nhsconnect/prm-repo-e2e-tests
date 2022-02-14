package uk.nhs.prm.deduction.e2e.tests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.nhs.prm.deduction.e2e.TestConfiguration;
import uk.nhs.prm.deduction.e2e.pdsadaptor.PdsAdaptorClient;
import uk.nhs.prm.deduction.e2e.pdsadaptor.PdsAdaptorResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.nhs.prm.deduction.e2e.nhs.NhsIdentityGenerator.generateRandomOdsCode;

public class PdsAdaptorTest {

    private PdsAdaptorClient pdsAdaptorClient;
    private TestConfiguration config;
    private String nhsNumber;

    @BeforeEach
    public void setup() {
        config = new TestConfiguration();
        pdsAdaptorClient = new PdsAdaptorClient(config);
        nhsNumber = config.getPdsAdaptorTestPatientNhsNumber();
    }

    @Test
    void shouldUpdateManagingOrganisationOfPatient() {
        PdsAdaptorResponse pdsAdaptorResponse = pdsAdaptorClient.getSuspendedPatientStatus(nhsNumber);
        String newOdsCode = generateRandomOdsCode();
        PdsAdaptorResponse pdsAdaptorUpdateResponse =
            pdsAdaptorClient.updateManagingOrganisation(nhsNumber, newOdsCode, pdsAdaptorResponse.getRecordETag());
        assertThat(pdsAdaptorUpdateResponse.getManagingOrganisation()).isEqualTo(newOdsCode);
    }

}
