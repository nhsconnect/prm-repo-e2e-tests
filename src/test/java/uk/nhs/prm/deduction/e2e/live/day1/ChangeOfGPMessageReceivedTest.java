package uk.nhs.prm.deduction.e2e.live.day1;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.scheduling.annotation.EnableScheduling;


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnableScheduling
public class ChangeOfGPMessageReceivedTest {

    @Test
    public void shouldMoveSingleSuspensionMessageFromMeshMailBoxToNemsIncomingQueue() {
       Assertions.assertThat(true);

    }

}
