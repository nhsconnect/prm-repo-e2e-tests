package uk.nhs.prm.e2etests.timing;

import org.springframework.stereotype.Component;

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
