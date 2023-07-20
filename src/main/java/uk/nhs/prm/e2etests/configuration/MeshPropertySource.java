package uk.nhs.prm.e2etests.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.nhs.prm.e2etests.services.SsmService;

@Component
public class MeshPropertySource extends AbstractSsmRetriever {
    @Value("${aws.configuration.ssm.parameters.mesh.mailboxId}")
    private String mailboxId;

    @Value("${aws.configuration.ssm.parameters.mesh.clientCert}")
    private String clientCert;

    @Value("${aws.configuration.ssm.parameters.mesh.clientKey}")
    private String clientKey;

    @Value("${aws.configuration.ssm.parameters.mesh.mailboxPassword}")
    private String mailboxPassword;

    private static final String MAILBOX_SERVICE_OUTBOX_URI = "https://msg.intspineservices.nhs.uk/messageexchange/%s/outbox";

    @Autowired
    public MeshPropertySource(SsmService ssmService) {
        super(ssmService);
    }

    public String getMailboxId() {
        return super.getAwsSsmParameterValue(this.mailboxId);
    }

    public String getClientCert() {
        return super.getAwsSsmParameterValue(this.clientCert);
    }

    public String getClientKey() {
        return super.getAwsSsmParameterValue(this.clientKey);
    }

    public String getMailboxPassword() {
        return super.getAwsSsmParameterValue(this.mailboxPassword);
    }

    public String getMailboxServiceOutboxUri() {
        return super.getAwsSsmParameterValue(
            String.format(MAILBOX_SERVICE_OUTBOX_URI, this.getMailboxId())
        );
    }
}