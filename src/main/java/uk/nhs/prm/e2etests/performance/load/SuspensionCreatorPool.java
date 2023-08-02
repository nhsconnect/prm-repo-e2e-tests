package uk.nhs.prm.e2etests.performance.load;

import uk.nhs.prm.e2etests.utility.NhsIdentityGenerator;
import uk.nhs.prm.e2etests.performance.NemsTestEvent;

public class SuspensionCreatorPool implements Pool<NemsTestEvent> {
    private final Pool<String> nhsNumberPool;

    public SuspensionCreatorPool(Pool<String> nhsNumberPool) {
        this.nhsNumberPool = nhsNumberPool;
    }

    public NemsTestEvent next() {
        var nhsNumber = nhsNumberPool.next();
        var nemsMessageId = NhsIdentityGenerator.randomNemsMessageId();
        return NemsTestEvent.suspensionEvent(nhsNumber, nemsMessageId);
    }
}
