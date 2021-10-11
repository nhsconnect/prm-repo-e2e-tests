package uk.nhs.prm.deduction.e2e.mesh;

import org.springframework.stereotype.Component;
import uk.nhs.prm.deduction.e2e.nems.NemsEventMessage;

@Component
public class MeshMailbox {
    public void postMessage(NemsEventMessage aMailboxMessage) {

    }

    public boolean isEmpty() {
        return false;
    }
}
