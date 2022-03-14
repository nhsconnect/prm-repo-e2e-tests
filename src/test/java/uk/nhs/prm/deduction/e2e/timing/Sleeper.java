package uk.nhs.prm.deduction.e2e.timing;

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
