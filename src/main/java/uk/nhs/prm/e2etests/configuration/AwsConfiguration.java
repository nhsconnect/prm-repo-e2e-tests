package uk.nhs.prm.e2etests.configuration;

import software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import uk.nhs.prm.e2etests.exception.ActiveRoleArnException;
import software.amazon.awssdk.services.sts.StsClient;
import org.springframework.context.annotation.Bean;
import lombok.extern.log4j.Log4j2;

import static software.amazon.awssdk.regions.Region.EU_WEST_2;

@Log4j2
@Configuration
public class AwsConfiguration {
    private static final String DEFAULT_VALUE_NO_ENVIRONMENT_VARIABLE_SET = "unset";

    @Value("${aws.configuration.requiredRoleArn}")
    private String requiredRoleArn;

    @Value("${aws.configuration.secretAccessKey}")
    private String secretAccessKey;

    @Value("${aws.configuration.accessKey}")
    private String accessKey;

    @Value("${aws.configuration.sessionToken}")
    private String sessionToken;

    @Value("${aws.configuration.region}")
    private String region;

    @Bean @ConditionalOnProperty(
            prefix = "aws.configuration",
            name = { "requiredRoleArn" },
            havingValue = DEFAULT_VALUE_NO_ENVIRONMENT_VARIABLE_SET
    )
    public AwsCredentialsProvider awsumeAwsCredentialsProvider() {
        return StaticCredentialsProvider
                .create(
                        AwsSessionCredentials
                                .create(this.accessKey, this.secretAccessKey, this.sessionToken)
                );
    }

    @Bean @ConditionalOnProperty(
            prefix = "aws.configuration",
            name = { "accessKey", "secretAccessKey" },
            havingValue = DEFAULT_VALUE_NO_ENVIRONMENT_VARIABLE_SET
    )
    public StsAssumeRoleCredentialsProvider assumeRoleAwsCredentialsProvider() {
        final StsClient stsClient = StsClient.builder().build();

        return StsAssumeRoleCredentialsProvider.builder()
                .stsClient(stsClient)
                .refreshRequest(request -> {
                    request.roleArn(activeRoleArn().getTargetArn());
                    request.roleSessionName("perf-test");
                })
                .build();
    }

    @Bean
    public ActiveRoleArn activeRoleArn() {
        if(this.requiredRoleArn.equalsIgnoreCase(DEFAULT_VALUE_NO_ENVIRONMENT_VARIABLE_SET)) {
            try(final StsClient stsClient = StsClient.builder()
                    .region(EU_WEST_2)
                    .credentialsProvider(awsumeAwsCredentialsProvider())
                    .build()) {
                return new ActiveRoleArn(stsClient.getCallerIdentity().arn());
            } catch (Exception exception) {
                throw new ActiveRoleArnException(exception.getMessage());
            }
        }
        else {
            return new ActiveRoleArn(this.requiredRoleArn);
        }
    }
}