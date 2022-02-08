package uk.nhs.prm.deduction.e2e.performance.load;

import uk.nhs.prm.deduction.e2e.performance.NemsTestEvent;
import uk.nhs.prm.deduction.e2e.utility.Helper;

public class SuspensionCreatorPool implements Pool<NemsTestEvent> {
    private final Pool<String> nhsNumberPool;
    private final Helper helper = new Helper();

    public SuspensionCreatorPool(Pool<String> nhsNumberPool) {
        this.nhsNumberPool = nhsNumberPool;
    }

    public NemsTestEvent next() {
        var nhsNumber = nhsNumberPool.next();
        var nemsMessageId = helper.randomNemsMessageId();
        var testEvent = new NemsTestEvent(nemsMessageId, nhsNumber);
        return testEvent;
    }
}
