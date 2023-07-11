package uk.nhs.prm.e2etests.mesh;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.nhs.prm.e2etests.client.RoleAssumingAwsConfigurationClient;
import uk.nhs.prm.e2etests.configuration.NHSConfiguration;

@Getter
@Component
public class MeshConfig {
    private final RoleAssumingAwsConfigurationClient roleAssumingAwsConfigurationClient;
    private final NHSConfiguration nhsConfiguration;
    private final String mailboxPassword;
    private final String mailboxId;
    private final String clientCert;
    private final String clientKey;

    @Autowired
    public MeshConfig(
            RoleAssumingAwsConfigurationClient roleAssumingAwsConfigurationClient,
            NHSConfiguration nhsConfiguration
    ) {
        this.roleAssumingAwsConfigurationClient = roleAssumingAwsConfigurationClient;
        this.nhsConfiguration = nhsConfiguration;
        this.mailboxId = roleAssumingAwsConfigurationClient.getSsmParameterValue(getValue("mesh-mailbox-id"));
        this.mailboxPassword = roleAssumingAwsConfigurationClient.getSsmParameterValue(getValue("mesh-mailbox-password"));
        this.clientCert = roleAssumingAwsConfigurationClient.getSsmParameterValue(getValue("mesh-mailbox-client-cert"));
        this.clientKey = roleAssumingAwsConfigurationClient.getSsmParameterValue(getValue("mesh-mailbox-client-key"));
    }

    private String getValue(String resource) {
        return this.roleAssumingAwsConfigurationClient.getSsmParameterValue(
            String.format("/repo/%s/user-input/external/%s", this.nhsConfiguration.getNhsEnvironment(), resource)
        );
    }
}