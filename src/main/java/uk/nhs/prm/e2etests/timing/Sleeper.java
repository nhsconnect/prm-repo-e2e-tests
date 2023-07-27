package uk.nhs.prm.e2etests.timing;

import org.springframework.stereotype.Component;

// TODO PRMT-3523: SURELY THIS DOES NOT NEED TO BE A COMPONENT? CAN'T WE JUST
// SLAP IT AS A PUBLIC FINAL CLASS AND HAVE THE SLEEP BE STATIC AND MOVE IT TO
// THE UTILITY DIR??
@Component
public class Sleeper {
    public long sleep(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        }
        catch (InterruptedException e) {
            System.err.println("sleep interrupted");
        }
        return System.currentTimeMillis();
    }
}
