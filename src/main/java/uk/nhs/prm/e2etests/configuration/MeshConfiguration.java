package uk.nhs.prm.e2etests.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import uk.nhs.prm.e2etests.services.SsmService;

@Configuration
public class MeshConfiguration extends AbstractSsmRetriever {
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
    public MeshConfiguration(SsmService ssmService) {
        super(ssmService);
    }

    public enum Type {
        MAILBOX_ID,
        CLIENT_CERT,
        CLIENT_KEY,
        MAILBOX_PASSWORD
    }

    public String getValue(Type type) {
        return switch (type) {
            case MAILBOX_ID -> super.getAwsSsmParameterValue(this.mailboxId);
            case CLIENT_CERT -> super.getAwsSsmParameterValue(this.clientCert);
            case CLIENT_KEY -> super.getAwsSsmParameterValue(this.clientKey);
            case MAILBOX_PASSWORD -> super.getAwsSsmParameterValue(this.mailboxPassword);
        };
    }

    public String getMailboxServiceOutboxUri() {
        final String value = super.getAwsSsmParameterValue(this.mailboxId);
        return super.getAwsSsmParameterValue(
            String.format(MAILBOX_SERVICE_OUTBOX_URI, value)
        );
    }
}