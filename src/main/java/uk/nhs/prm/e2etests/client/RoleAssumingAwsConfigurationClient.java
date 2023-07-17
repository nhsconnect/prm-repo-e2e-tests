package uk.nhs.prm.e2etests.client;

import lombok.AllArgsConstructor;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.ssm.SsmClient;
import org.springframework.beans.factory.annotation.Autowired;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;

@Component
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class RoleAssumingAwsConfigurationClient {
    private final SsmClient ssmClient;
    private final Logger LOGGER = LogManager.getLogger(RoleAssumingAwsConfigurationClient.class);

    public String getSsmParameterValue(String paramName) {
        LOGGER.info("Fetching AWS SSM parameter value: {}", paramName);
        GetParameterResponse parameterResponse = ssmClient.getParameter(GetParameterRequest.builder()
                .name(paramName)
                .withDecryption(true)
                .build());
        return parameterResponse.parameter().value();
    }
}