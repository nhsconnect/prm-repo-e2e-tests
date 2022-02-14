package uk.nhs.prm.deduction.e2e.performance.load;

import uk.nhs.prm.deduction.e2e.nhs.NhsIdentityGenerator;
import uk.nhs.prm.deduction.e2e.performance.NemsTestEvent;
import uk.nhs.prm.deduction.e2e.utility.QueueHelper;

public class SuspensionCreatorPool implements Pool<NemsTestEvent> {
    private final Pool<String> nhsNumberPool;
    private final QueueHelper helper = new QueueHelper();

    public SuspensionCreatorPool(Pool<String> nhsNumberPool) {
        this.nhsNumberPool = nhsNumberPool;
    }

    public NemsTestEvent next() {
        var nhsNumber = nhsNumberPool.next();
        var nemsMessageId = NhsIdentityGenerator.randomNemsMessageId(false);
        var testEvent = NemsTestEvent.suspensionEvent(nhsNumber, nemsMessageId);
        return testEvent;
    }
}
