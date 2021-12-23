package uk.nhs.prm.deduction.e2e.tests;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.nhs.prm.deduction.e2e.pdsadaptor.PdsAdaptorClient;
import uk.nhs.prm.deduction.e2e.pdsadaptor.PdsAdaptorResponse;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = PdsAdaptorClient.class)
@ExtendWith(SpringExtension.class)
public class PdsAdaptorTest {

    @Autowired
    private PdsAdaptorClient pdsAdaptorClient;

    @Test
    void shouldUpdateManagingOrganisationOfPatient() {
        PdsAdaptorResponse pdsAdaptorResponse = pdsAdaptorClient.getSuspendedPatientStatus();
        String newOdsCode = generateRandomOdsCode();
        PdsAdaptorResponse pdsAdaptorUpdateResponse =
            pdsAdaptorClient.updateManagingOrganisation(newOdsCode, pdsAdaptorResponse.getRecordETag());
        assertThat(pdsAdaptorUpdateResponse.getManagingOrganisation()).isEqualTo(newOdsCode);
    }

    private String generateRandomOdsCode() {
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
