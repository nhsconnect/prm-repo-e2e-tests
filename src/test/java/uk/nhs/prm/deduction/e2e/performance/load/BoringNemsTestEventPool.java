package uk.nhs.prm.deduction.e2e.performance.load;

import uk.nhs.prm.deduction.e2e.performance.NemsTestEvent;

public class BoringNemsTestEventPool implements Pool<NemsTestEvent> {
    private final NemsTestEvent event;

    public BoringNemsTestEventPool(NemsTestEvent event) {
        this.event = event;
    }

    @Override
    public NemsTestEvent next() {
        return event;
    }
}
