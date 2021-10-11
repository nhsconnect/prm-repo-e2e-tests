package uk.nhs.prm.deduction.e2e.queue;
import org.apache.http.HttpException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = SQSClient.class )
public class SQSClient {

    @Test
    public void firstTest() throws HttpException {


//        Utils.debugSystemProperties();
//
//        MeshClient meshClient = new MeshClient();
//         meshClient.postMessageToMeshMailbox();
//        AmazonSQS sqs = AmazonSQSClientBuilder.defaultClient();
//        ListQueuesResult lq_result = sqs.listQueues();
//        System.out.println("https://sqs.eu-west-2.amazonaws.com/416874859154/dev-mesh-forwarder-nems-events-observability-queue");
//        for (String url : lq_result.getQueueUrls()) {
//            System.out.println(url);
//        }
    }

}