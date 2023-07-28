package uk.nhs.prm.e2etests.performance.load;

import uk.nhs.prm.e2etests.performance.NemsTestEvent;

public class NemsTestEventPool implements Pool<NemsTestEvent> {
    private final NemsTestEvent event;

    public NemsTestEventPool(NemsTestEvent event) {
        this.event = event;
    }

    @Override
    public NemsTestEvent next() {
        return event;
    }
}
