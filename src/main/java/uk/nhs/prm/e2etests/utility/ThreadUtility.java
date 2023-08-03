package uk.nhs.prm.e2etests.utility;

import uk.nhs.prm.e2etests.exception.GenericException;
import lombok.extern.log4j.Log4j2;

@Log4j2
public final class ThreadUtility {
    private ThreadUtility() { }

    public static long sleepFor(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
            return System.currentTimeMillis();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new GenericException(ThreadUtility.class.getName(), exception.getMessage());
        }
    }
}