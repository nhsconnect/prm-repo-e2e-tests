package uk.nhs.prm.deduction.e2e.tests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.nhs.prm.deduction.Wiring;
import uk.nhs.prm.deduction.e2e.mesh.MeshMailbox;
import uk.nhs.prm.deduction.e2e.nems.NemsEventMessage;
import uk.nhs.prm.deduction.e2e.nems.NemsEventMessageQueue;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EndToEndTest {

    private Wiring wiring = new Wiring();

    private NemsEventMessageQueue meshForwarderQueue;
    private MeshMailbox meshMailbox;

    @BeforeEach
    public void wireUp() throws Exception {
        meshForwarderQueue = wiring.meshForwarderQueue();
        meshMailbox = wiring.meshMailbox();
    }

    @Test
    public void theSystemShouldMoveMessagesFromOurMeshMailboxOntoAQueue() throws Exception {
        NemsEventMessage nemsEventMessage = someNemsEvent("1234567890");

        String postedMessageId  = meshMailbox.postMessage(nemsEventMessage);

        assertThat(meshForwarderQueue.readEventMessage().body()).contains("1234567890");
        assertFalse(meshMailbox.hasMessageId(postedMessageId));
    }

    private NemsEventMessage someNemsEvent(String nhsNumber) {
        return new NemsEventMessage("dummy message for nhs number: " + nhsNumber);
    }

}
