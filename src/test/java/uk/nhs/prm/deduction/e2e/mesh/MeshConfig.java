package uk.nhs.prm.deduction.e2e.mesh;

import lombok.Getter;
import uk.nhs.prm.deduction.e2e.TestConfiguration;

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
