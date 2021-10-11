package uk.nhs.prm.deduction.e2e.queue;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.ListQueuesResult;
import org.apache.http.HttpException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

public class SQSClient {


    public void () throws HttpException {

        AmazonSQS sqs = AmazonSQSClientBuilder.defaultClient();
        ListQueuesResult lq_result = sqs.listQueues();
        System.out.println("https://sqs.eu-west-2.amazonaws.com/416874859154/dev-mesh-forwarder-nems-events-observability-queue");
        for (String url : lq_result.getQueueUrls()) {
            System.out.println(url);
        }
    }

    public String readMessageFrom(String queueUri) {
        AmazonSQS sqs = AmazonSQSClientBuilder.defaultClient();
        return sqs.receiveMessage(queueUri).;
    }
}