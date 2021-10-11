package uk.nhs.prm.deduction.e2e.tests;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import uk.nhs.prm.deduction.e2e.mesh.MeshMailbox;
import uk.nhs.prm.deduction.e2e.nems.NemsEventMessage;
import uk.nhs.prm.deduction.e2e.nems.NemsEventMessageQueue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EndToEndTest {

    private NemsEventMessageQueue meshForwarderQueue = new NemsEventMessageQueue();

    @Disabled("in progress")
    @Test
    public void theSystemShouldMoveMessagesFromOurMeshMailboxOntoAQueue() {
        NemsEventMessage nemsEventMessage = someNemsEvent("1234567890");

        meshMailbox().postMessage(nemsEventMessage);

        assertEquals(meshForwarderQueue.readEventMessage().nhsNumber(), "1234567890");
        assertTrue(meshMailbox().isEmpty());
    }

    private NemsEventMessage someNemsEvent(String nhsNumber) {
        return new NemsEventMessage();
    }

    private MeshMailbox meshMailbox() {
        return new MeshMailbox();
    }
}
