package uk.nhs.prm.e2etests.utility;

import lombok.extern.log4j.Log4j2;
import uk.nhs.prm.e2etests.annotation.Debt;
import uk.nhs.prm.e2etests.exception.GenericException;

import static uk.nhs.prm.e2etests.annotation.Debt.Priority.MEDIUM;

@Log4j2
@Debt(comment = "We would like to get rid of this at some point in the future.", priority = MEDIUM)
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