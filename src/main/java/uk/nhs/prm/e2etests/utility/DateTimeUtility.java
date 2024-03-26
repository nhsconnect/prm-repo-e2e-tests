package uk.nhs.prm.e2etests.utility;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class DateTimeUtility {

    private static final String ZONE_ID = "Europe/London";

    public static String getStartOfYearDateTimeAsString() {
        LocalDateTime startOfyear = LocalDateTime.of(2024,1, 1, 1, 1, 1);

        return ZonedDateTime.of(startOfyear, ZoneId.of(ZONE_ID))
                .truncatedTo(ChronoUnit.MINUTES)
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }
}
