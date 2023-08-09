package uk.nhs.prm.e2etests.utility;

import com.google.gson.Gson;
import uk.nhs.prm.e2etests.model.SqsMessage;
import uk.nhs.prm.e2etests.model.nems.NemsResolutionMessage;

public final class MappingUtility {
    private MappingUtility() { }

    public static NemsResolutionMessage mapToNemsResolutionMessage(SqsMessage jsonBody) {
        return new Gson().fromJson(
                jsonBody.getBody(),
                NemsResolutionMessage.class
        );
    }
}
