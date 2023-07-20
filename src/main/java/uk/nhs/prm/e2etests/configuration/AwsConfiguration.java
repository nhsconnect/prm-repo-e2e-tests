package uk.nhs.prm.e2etests.configuration;

import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import org.springframework.context.annotation.Configuration;
import uk.nhs.prm.e2etests.exception.UnknownRegionException;
import org.springframework.beans.factory.annotation.Value;
import uk.nhs.prm.e2etests.exception.AssumedRoleException;
import software.amazon.awssdk.services.sts.StsClient;
import org.springframework.context.annotation.Bean;
import uk.nhs.prm.e2etests.ExampleAssumedRoleArn;
import software.amazon.awssdk.regions.Region;
import jakarta.annotation.PostConstruct;

import java.util.regex.Pattern;

@Configuration
public class AwsConfiguration {
    @Value("${aws.configuration.requiredRoleArn}")
    private String requiredRoleArn;

    @Value("${aws.configuration.secretAccessKey}")
    private String secretAccessKey;

    @Value("${aws.configuration.accessKey}")
    private String accessKey;

    @Value("${aws.configuration.sessionToken}")
    private String sessionToken;

    @Value("${aws.configuration.region:#{'eu-west-2'}}")
    private String region;

    @Bean
    public AwsCredentialsProvider awsCredentialsProvider() {
        return StaticCredentialsProvider
                .create(
                        AwsSessionCredentials
                                .create(this.accessKey, this.secretAccessKey, this.sessionToken)
                );
    }

    @Bean
    public ExampleAssumedRoleArn exampleAssumedRoleArn() {
        try(StsClient stsClient = StsClient
                .builder()
                .region(Region.EU_WEST_2)
                .credentialsProvider(awsCredentialsProvider())
                .build()) {

            if(this.requiredRoleArn.isBlank()) return new ExampleAssumedRoleArn(stsClient.getCallerIdentity().arn());
            else return new ExampleAssumedRoleArn(this.requiredRoleArn);
        } catch (Exception exception) {
            throw new AssumedRoleException(exception.getMessage());
        }
    }

    @PostConstruct
    private void validateAwsRegion() {
        final Pattern pattern = Pattern.compile("(us(-gov)?|ap|ca|cn|eu|sa)-(central|(north|south)?(east|west)?)-\\d");
        if(!pattern.matcher(this.region).find()) throw new UnknownRegionException();
    }
}