package uk.nhs.prm.deduction;

import uk.nhs.prm.deduction.e2e.TestConfiguration;
import uk.nhs.prm.deduction.e2e.client.AwsConfigurationClient;
import uk.nhs.prm.deduction.e2e.nems.NemsEventMessageQueue;
import uk.nhs.prm.deduction.e2e.queue.SqsQueue;

public class Wiring {
    private TestConfiguration configuration = new TestConfiguration();

    public AwsConfigurationClient awsConfigurationClient() {
        return new AwsConfigurationClient();
    }

    public NemsEventMessageQueue meshForwarderQueue() {
        return new NemsEventMessageQueue(sqsQueue(), configuration.meshForwarderObservabilityQueueUri());
    }

    private SqsQueue sqsQueue() {
        return new SqsQueue();
    }
}
