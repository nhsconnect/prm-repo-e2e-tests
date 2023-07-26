package uk.nhs.prm.e2etests.configuration;

import software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;
import org.springframework.scheduling.annotation.EnableScheduling;
import uk.nhs.prm.e2etests.exception.UnknownAwsRegionException;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import uk.nhs.prm.e2etests.exception.AssumedRoleException;
import software.amazon.awssdk.services.sts.StsClient;
import org.springframework.context.annotation.Bean;
import uk.nhs.prm.e2etests.ExampleAssumedRoleArn;
import jakarta.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;

import java.util.regex.Pattern;

import static software.amazon.awssdk.regions.Region.EU_WEST_2;

@Log4j2
@Configuration
@EnableScheduling
public class AwsConfiguration {
    private static final String AWS_REGION_REGEX = "(us(-gov)?|ap|ca|cn|eu|sa)-(central|(north|south)?(east|west)?)-\\d";
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

    @Bean
    @ConditionalOnProperty(
            prefix = "aws.configuration",
            name = { "accessKey", "secretAccessKey" },
            havingValue = DEFAULT_VALUE_NO_ENVIRONMENT_VARIABLE_SET
    )
    public AwsCredentialsProvider assumeRoleAwsCredentialsProvider() {
        try(final StsClient stsClient = StsClient.builder()
                .region(EU_WEST_2)
                .build()) {
            final AssumeRoleRequest assumeRoleRequest = AssumeRoleRequest.builder()
                    .roleArn(exampleAssumedRoleArn().getTargetArn())
                    .roleSessionName("perf-test")
                    .build();

            return StsAssumeRoleCredentialsProvider
                    .builder()
                    .stsClient(stsClient)
                    .refreshRequest(assumeRoleRequest)
                    .build();
        }
    }

    @Bean
    public ExampleAssumedRoleArn exampleAssumedRoleArn() {
        if(this.requiredRoleArn.equalsIgnoreCase(DEFAULT_VALUE_NO_ENVIRONMENT_VARIABLE_SET)) {
            try(final StsClient stsClient = StsClient.builder()
                    .region(EU_WEST_2)
                    .credentialsProvider(awsumeAwsCredentialsProvider())
                    .build()) {
                return new ExampleAssumedRoleArn(stsClient.getCallerIdentity().arn());
            } catch (Exception exception) {
                throw new AssumedRoleException(exception.getMessage());
            }
        }
        else {
            return new ExampleAssumedRoleArn(this.requiredRoleArn);
        }
    }

    @PostConstruct
    private void validateAwsRegion() {
        final Pattern pattern = Pattern.compile(AWS_REGION_REGEX);
        if(!pattern.matcher(this.region).find()) throw new UnknownAwsRegionException();
    }
}