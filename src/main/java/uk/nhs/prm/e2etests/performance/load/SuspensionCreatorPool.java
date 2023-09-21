package uk.nhs.prm.e2etests.performance.load;

import uk.nhs.prm.e2etests.performance.NemsTestEvent;
import uk.nhs.prm.e2etests.utility.TestDataUtility;

public class SuspensionCreatorPool implements Pool<NemsTestEvent> {
    private final Pool<String> nhsNumberPool;

    public SuspensionCreatorPool(Pool<String> nhsNumberPool) {
        this.nhsNumberPool = nhsNumberPool;
    }

    public NemsTestEvent next() {
        String nhsNumber = nhsNumberPool.next();
        String nemsMessageId = TestDataUtility.randomNemsMessageId();
        return NemsTestEvent.suspensionEvent(nhsNumber, nemsMessageId);
    }
}
