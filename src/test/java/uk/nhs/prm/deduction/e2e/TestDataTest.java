package uk.nhs.prm.deduction.e2e;

import org.junit.jupiter.api.Test;
import uk.nhs.prm.e2etests.TestData;

import static org.assertj.core.api.Assertions.assertThat;

public class TestDataTest {

    @Test
    void shouldGenerateRandom10DigitNumbersForPerfEnv() {
        var perfData = TestData.perf(15);
        assertThat(perfData).hasSize(15);

        var distinctCount = perfData.stream().distinct().count();
        assertThat(distinctCount).isEqualTo(15);
        assertThat(perfData).allMatch(s -> s.length() == 10);
        assertThat(perfData).allMatch(s -> s.startsWith("969"));
    }
}
