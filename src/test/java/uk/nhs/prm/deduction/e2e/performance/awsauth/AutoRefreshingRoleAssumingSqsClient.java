package uk.nhs.prm.deduction.e2e.performance.awsauth;

import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.services.sqs.SqsClient;
import uk.nhs.prm.deduction.e2e.queue.BasicSqsClient;

import java.util.concurrent.TimeUnit;

@Component
@Lazy
public class AutoRefreshingRoleAssumingSqsClient extends BasicSqsClient {

    private AssumeRoleCredentialsProviderFactory credentialsProviderFactory;

    public AutoRefreshingRoleAssumingSqsClient(AssumeRoleCredentialsProviderFactory credentialsProviderFactory) {
        this.credentialsProviderFactory = credentialsProviderFactory;
        provideWarnings();
        setNewSqsClient();
    }

    @Scheduled(fixedRate = 10, initialDelay = 10, timeUnit = TimeUnit.SECONDS)
    public void setNewSqsClient() {
        System.out.println("Refreshing SQS client in " + getClass());
        AwsCredentialsProvider credentialsProvider = credentialsProviderFactory.createProvider();
        SqsClient sqsClient = SqsClient.builder()
                .credentialsProvider(credentialsProvider)
                .build();
        this.setSqsClient(sqsClient);
    }

    private void provideWarnings() {
        System.err.println("WARNING: Using AWS credentials auto-refresh assume-role in " + getClass() +
                " - this is not going to work locally from base user credentials!");
        checkNotUsingStaticEnvironmentAuth();
    }

    private void checkNotUsingStaticEnvironmentAuth() {
        if (System.getenv("AWS_ACCESS_KEY_ID") != null) {
            System.err.println("WATCH OUT! Setup to assume-role on auto-refresh (creating " + getClass() +
                    "), but currently using time-limited env auth (AWS_ACCESS_KEY_ID is set) " +
                    "so this will fail after the base credentials expire...");
        }
    }
}
