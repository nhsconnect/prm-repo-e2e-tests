package uk.nhs.prm.deduction.e2e.mesh;

import lombok.Getter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.utils.ImmutableMap;
import uk.nhs.prm.deduction.e2e.TestConfiguration;
import uk.nhs.prm.deduction.e2e.client.AwsConfigurationClient;

import java.util.List;

import static java.util.Arrays.asList;

@Getter
public class MeshConfig {
    private final String mailboxId;
    private final String mailboxPassword;
    private final String clientCert;
    private final String clientKey;

    public MeshConfig(TestConfiguration configuration) {
        this.mailboxId = configuration.getMeshMailBoxID();
        this.mailboxPassword = configuration.getMeshMailBoxPassword();
        this.clientCert = configuration.getMeshMailBoxClientCert();
        this.clientKey = configuration.getMeshMailBoxClientKey();
    }
}
