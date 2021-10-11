package uk.nhs.prm.deduction.e2e.client;

import org.springframework.stereotype.Component;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;
import software.amazon.awssdk.services.ssm.model.SsmException;
@Component
public class AwsConfigurationClient {
    private SsmClient ssmClient;

    public AwsConfigurationClient() {
        Region region =Region.EU_WEST_2;
        ssmClient = SsmClient.builder()
                .region(region)
                .build();
    }

    public String getParaValue(String paraName) {

        try {
            GetParameterRequest parameterRequest = GetParameterRequest.builder()
                    .name(paraName)
                    .build();

            GetParameterResponse parameterResponse = ssmClient.getParameter(parameterRequest);
            System.out.println("The parameter value is " + parameterResponse.parameter().value());
            return parameterResponse.parameter().value();

        } catch (SsmException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
        return paraName;
    }
}