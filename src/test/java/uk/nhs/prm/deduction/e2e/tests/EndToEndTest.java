package uk.nhs.prm.deduction.e2e.tests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.nhs.prm.deduction.Wiring;
import uk.nhs.prm.deduction.e2e.mesh.MeshMailbox;
import uk.nhs.prm.deduction.e2e.nems.NemsEventMessage;
import uk.nhs.prm.deduction.e2e.nems.NemsEventMessageQueue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EndToEndTest {

    private Wiring wiring = new Wiring();

    private NemsEventMessageQueue meshForwarderQueue;
    private MeshMailbox meshMailbox;

    @BeforeEach
    public void wireUp() {
        meshForwarderQueue = wiring.meshForwarderQueue();
        meshMailbox = new MeshMailbox();
    }

    //    @Disabled("in progress")
    @Test
    public void theSystemShouldMoveMessagesFromOurMeshMailboxOntoAQueue() {
        NemsEventMessage nemsEventMessage = someNemsEvent("1234567890");

        meshMailbox.postMessage(nemsEventMessage);

        assertEquals(meshForwarderQueue.readEventMessage().nhsNumber(), "1234567890");
        assertTrue(meshMailbox.isEmpty());
    }

    private NemsEventMessage someNemsEvent(String nhsNumber) {
        return new NemsEventMessage();
    }

}
