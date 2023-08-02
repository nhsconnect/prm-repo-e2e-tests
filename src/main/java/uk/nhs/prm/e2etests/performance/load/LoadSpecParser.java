package uk.nhs.prm.e2etests.performance.load;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Integer.parseInt;

public class LoadSpecParser {
    public static List<LoadPhase> parsePhases(String loadSpecString) {
        List<LoadPhase> phases = new ArrayList<>();
        String[] phaseSpecs = loadSpecString.split(",");
        for (String phaseSpec : phaseSpecs) {
            String[] countAndRate = phaseSpec.split("@");
            phases.add(LoadPhase.atFlatRate(parseInt(countAndRate[0]), countAndRate[1]));
        }
        return phases;
    }
}
