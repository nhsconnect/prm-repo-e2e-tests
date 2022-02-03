package uk.nhs.prm.deduction.e2e.tests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.nhs.prm.deduction.e2e.TestConfiguration;
import uk.nhs.prm.deduction.e2e.pdsadaptor.PdsAdaptorClient;
import uk.nhs.prm.deduction.e2e.pdsadaptor.PdsAdaptorResponse;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

public class PdsAdaptorTest {

    private PdsAdaptorClient pdsAdaptorClient;
    private TestConfiguration config;
    private String nhsNumber;

    @BeforeEach
    public void setup() {
        config = new TestConfiguration();
        pdsAdaptorClient = new PdsAdaptorClient();
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

    public static String generateRandomOdsCode() {
        int leftLimit = 48; // numeral '0'
        int rightLimit = 122; // letter 'z'
        int targetStringLength = 5;
        Random random = new Random();

        String generatedString = random.ints(leftLimit, rightLimit + 1)
            .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
            .limit(targetStringLength)
            .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
            .toString();
        System.out.printf("Generating random ods code: %s%n", generatedString);
        return generatedString;
    }
}
