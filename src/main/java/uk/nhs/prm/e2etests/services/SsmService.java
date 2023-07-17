package uk.nhs.prm.e2etests.services;

import software.amazon.awssdk.services.ssm.model.GetParameterResponse;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import org.springframework.beans.factory.annotation.Autowired;
import software.amazon.awssdk.services.ssm.SsmClient;
import org.springframework.stereotype.Service;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class SsmService {
    private final SsmClient ssmClient;

    public String getSsmParameterValue(String paramName) {
        GetParameterResponse parameterResponse = ssmClient.getParameter(GetParameterRequest.builder()
                .name(paramName)
                .withDecryption(true)
                .build());

        return parameterResponse.parameter().value();
    }
}