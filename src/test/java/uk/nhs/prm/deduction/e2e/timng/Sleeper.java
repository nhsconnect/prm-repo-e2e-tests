package uk.nhs.prm.deduction.e2e.timng;

public class Sleeper {
    public void sleep(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        }
        catch (InterruptedException e) {
            System.err.println("sleep interrupted");
        }
    }
}
