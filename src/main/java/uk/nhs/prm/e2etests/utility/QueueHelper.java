package uk.nhs.prm.e2etests.utility;

import uk.nhs.prm.e2etests.model.nems.NemsResolutionMessage;
import uk.nhs.prm.e2etests.model.SqsMessage;
import com.google.gson.Gson;

public final class QueueHelper {
    private QueueHelper() { }

    public static NemsResolutionMessage mapToNemsResolutionMessage(SqsMessage jsonBody) {
        return new Gson().fromJson(
                jsonBody.getBody(),
                NemsResolutionMessage.class
        );
    }
}
