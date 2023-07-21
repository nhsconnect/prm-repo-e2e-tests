package uk.nhs.prm.e2etests.performance.load;

import uk.nhs.prm.e2etests.performance.NemsTestEvent;

// TODO PRMT-3488 why is this 'boring'?
//  Is this unexciting? Digging a hole? A technical term as part of NEMS that we aren't aware of?
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
