package uk.nhs.prm.deduction.e2e.utility;

import org.json.JSONException;
import org.json.JSONObject;
import uk.nhs.prm.deduction.e2e.models.NonSensitiveDataMessage;
import uk.nhs.prm.deduction.e2e.queue.SqsMessage;

public class QueueHelper {

    public static  NonSensitiveDataMessage getNonSensitiveDataMessage(SqsMessage jsonBody) throws JSONException {
        JSONObject messageOnQueue = new JSONObject(jsonBody.body());
        String nemsMessageId = messageOnQueue.get("nemsMessageId").toString();
        String status = messageOnQueue.get("status").toString();
        return new NonSensitiveDataMessage(nemsMessageId,status);
    }

    public static boolean checkIfMessageIsExpectedMessage(NonSensitiveDataMessage expectedMessage, NonSensitiveDataMessage actualMessage) {
        if(expectedMessage.getNemsMessageId().equalsIgnoreCase(actualMessage.getNemsMessageId()) &&
                expectedMessage.getStatus().equalsIgnoreCase(actualMessage.getStatus())){
            return true;
        } else return false;
    }
}
