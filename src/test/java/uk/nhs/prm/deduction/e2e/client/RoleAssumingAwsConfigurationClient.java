package uk.nhs.prm.deduction.e2e.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;
import software.amazon.awssdk.services.ssm.model.SsmException;
import uk.nhs.prm.deduction.e2e.performance.awsauth.AssumeRoleCredentialsProviderFactory;

@Component
@Slf4j
public class RoleAssumingAwsConfigurationClient implements AwsConfigurationClient {
    private SsmClient ssmClient;

    public RoleAssumingAwsConfigurationClient(AssumeRoleCredentialsProviderFactory credentialsProviderFactory) {
        this.ssmClient = SsmClient.builder()
                .credentialsProvider(credentialsProviderFactory.createProvider())
                .region(Region.EU_WEST_2)
                .build();
    }

    public String getParamValue(String paramName) {
        System.out.println("Getting param value from ssm: " + paramName);
        try {
            GetParameterRequest parameterRequest = GetParameterRequest.builder()
                    .name(paramName)
                    .withDecryption(true)
                    .build();

            GetParameterResponse parameterResponse = ssmClient.getParameter(parameterRequest);
            return parameterResponse.parameter().value();

        } catch (SsmException e) {
            System.err.println(String.format("Error for ssm parameter %s and error is %s", paramName, e.getMessage()));
            throw e;
        }
    }
}
