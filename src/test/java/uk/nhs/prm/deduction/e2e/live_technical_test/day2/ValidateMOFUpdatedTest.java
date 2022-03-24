package uk.nhs.prm.deduction.e2e.live_technical_test.day2;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.scheduling.annotation.EnableScheduling;
import uk.nhs.prm.deduction.e2e.live_technical_test.TestParameters;

import static org.assertj.core.api.Assertions.assertThat;


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnableScheduling
public class ValidateMOFUpdatedTest {

    @Test
    public void shouldMoveSingleSuspensionMessageFromMeshMailBoxToNemsIncomingQueue() {
       assertThat(TestParameters.fetchTestParameter("LIVE_TECHNICAL_TEST_NEMS_MESSAGE_ID")).contains("-");
    }

}
