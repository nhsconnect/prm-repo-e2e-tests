package uk.nhs.prm.deduction.e2e;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.nhs.prm.deduction.e2e.client.AwsConfigurationClient;

@Component
public class Configuration {

    @Autowired
    private AwsConfigurationClient awsConfigurationClient;

    public String getMeshMailBoxID(){
        String latestStringToken = awsConfigurationClient.getParaValue("/repo/dev/user-input/external/mesh-mailbox-id");
        return latestStringToken;
    }

}
