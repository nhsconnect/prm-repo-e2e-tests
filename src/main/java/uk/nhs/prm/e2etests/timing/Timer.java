package uk.nhs.prm.e2etests.timing;

// TODO PRMT-3523: CAN'T WE JUST SLAP IT AS A PUBLIC FINAL CLASS
// AND HAVE THE milliseconds BE STATIC AND MOVE IT TO
// THE UTILITY DIR -> proceed to statically import??

public class Timer {
    public long milliseconds() {
        return System.currentTimeMillis();
    }
}
