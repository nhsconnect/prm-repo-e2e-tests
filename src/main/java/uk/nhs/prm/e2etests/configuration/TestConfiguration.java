package uk.nhs.prm.e2etests.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.nhs.prm.e2etests.annotation.Debt;
import uk.nhs.prm.e2etests.performance.load.LoadPhase;
import uk.nhs.prm.e2etests.performance.load.LoadSpecParser;

import java.util.List;

import static java.lang.Integer.parseInt;
import static java.lang.System.getenv;

@Debt(comment = "We'd prefer to remove this configuration class entirely, favouring breaking the logic out into " +
    "properties and configuration classes, though the remaining components are tightly coupled and don't harm the " +
    "future extensibility of the project.")
@Component
public class TestConfiguration {

    @Value("${test.performanceTestTimeout}")
    private String performanceTestTimeout;

    public int getPerformanceTestTimeout() {
        return parseInt(performanceTestTimeout);
    }

    @Debt(comment = "Trying to break this into a properties file involves addressing the 'LoadPhase' behaviour and" +
            "it's quite a job to work out what that is doing.")
    public List<LoadPhase> performanceTestLoadPhases(List<LoadPhase> defaultLoadPhases) {
        String loadSpec = getenv("PERFORMANCE_LOAD_SPEC");
        if (loadSpec == null) {
            return defaultLoadPhases;
        }
        return LoadSpecParser.parsePhases(loadSpec);
    }
}
