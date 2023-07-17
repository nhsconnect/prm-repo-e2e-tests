package uk.nhs.prm.e2etests.mesh.auth;

import uk.nhs.prm.e2etests.mesh.MeshConfig;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Formatter;
import java.util.UUID;

import static uk.nhs.prm.e2etests.configuration.MeshConfiguration.Type.MAILBOX_ID;
import static uk.nhs.prm.e2etests.configuration.MeshConfiguration.Type.MAILBOX_PASSWORD;

@Component
public class AuthTokenGenerator {
    private static final String HMAC_SHA216 = "HmacSHA256";
    private static final String AUTHSCHEMANAME = "NHSMESH";
    private static final String env_shared = "BackBone";
    private final String mailboxId;
    private final String mailboxPassword;

    @Autowired
    public AuthTokenGenerator(MeshConfiguration meshConfiguration) {
        this.mailboxId = meshConfiguration.getValue(MAILBOX_ID);
        this.mailboxPassword = meshConfiguration.getValue(MAILBOX_PASSWORD);
    }

    private String toHexString(byte[] bytes) {
        Formatter formatter = new Formatter();
        for (byte b : bytes) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }

    private String calculateHMAC(String data, String key) throws NoSuchAlgorithmException, InvalidKeyException {
       SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(), HMAC_SHA216);
        Mac mac = Mac.getInstance(HMAC_SHA216);
        mac.init(secretKeySpec);
        return toHexString(mac.doFinal(data.getBytes()));
    }

    public String getAuthorizationToken() throws Exception {
        final String timeStamp = new SimpleDateFormat("YmdHMS").format(Calendar.getInstance().getTime());
        final String nonce = generateNonce();
        final String nonce_count = "0";
        final String hmac_msg = this.mailboxId + ":" + nonce + ":" + nonce_count + ":" + this.mailboxPassword + ":" + timeStamp;
        final String hmac = calculateHMAC(hmac_msg, env_shared);
        final String token = AUTHSCHEMANAME + " " + this.mailboxId + ":" + nonce + ":" + nonce_count + ":" + timeStamp + ":" + hmac;
        return token;
    }

    private String generateNonce() {
        return UUID.randomUUID().toString();
    }
}