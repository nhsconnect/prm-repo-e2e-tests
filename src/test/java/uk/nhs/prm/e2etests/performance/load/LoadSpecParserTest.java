package uk.nhs.prm.e2etests.performance.load;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LoadSpecParserTest {

    @Test
    void shouldParseStringOfTotalCountsAtRatesIntoListOfPhases() {
        List<LoadPhase> phases = LoadSpecParser.parsePhases("20@0.2,30@0.4");

        assertThat(phases.get(0).totalCount).isEqualTo(20);
        assertThat(phases.get(0).ratePerSecond).isEqualTo(new BigDecimal("0.2"));
        assertThat(phases.get(1).totalCount).isEqualTo(30);
        assertThat(phases.get(1).ratePerSecond).isEqualTo(new BigDecimal("0.4"));
        assertThat(phases.size()).isEqualTo(2);
    }
}