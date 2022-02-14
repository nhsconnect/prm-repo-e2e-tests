package uk.nhs.prm.deduction.e2e.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;
import software.amazon.awssdk.services.ssm.model.SsmException;

@Component
@Slf4j
public class BasicAwsConfigurationClient implements AwsConfigurationClient {
    private SsmClient ssmClient;

    public BasicAwsConfigurationClient() {
        ssmClient = SsmClient.builder()
                .region(Region.EU_WEST_2)
                .build();
    }

    public void setSsmClient(SsmClient ssmClient) {
        this.ssmClient = ssmClient;
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
