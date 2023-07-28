package uk.nhs.prm.e2etests.utility;

import com.google.gson.Gson;
import uk.nhs.prm.e2etests.model.nems.NemsResolutionMessage;
import uk.nhs.prm.e2etests.model.SqsMessage;

public final class QueueHelper {
    public static NemsResolutionMessage getNonSensitiveDataMessage(SqsMessage jsonBody) {
        return new Gson()
                .fromJson(jsonBody.getBody(), NemsResolutionMessage.class);
    }

    public static boolean checkIfMessageIsExpectedMessage(NemsResolutionMessage expectedMessage, NemsResolutionMessage actualMessage) {
        return expectedMessage.getNemsMessageId().equalsIgnoreCase(actualMessage.getNemsMessageId())
                && expectedMessage.getStatus().equalsIgnoreCase(actualMessage.getStatus());
    }
}
