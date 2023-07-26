package uk.nhs.prm.e2etests.configuration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import uk.nhs.prm.e2etests.exception.UnknownAwsRegionException;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import uk.nhs.prm.e2etests.exception.AssumedRoleException;
import software.amazon.awssdk.services.sts.StsClient;
import org.springframework.context.annotation.Bean;
import uk.nhs.prm.e2etests.ExampleAssumedRoleArn;
import jakarta.annotation.PostConstruct;

import java.util.regex.Pattern;

import static software.amazon.awssdk.regions.Region.EU_WEST_2;

@Configuration
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