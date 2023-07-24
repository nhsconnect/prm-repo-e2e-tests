package uk.nhs.prm.e2etests.mesh.auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.nhs.prm.e2etests.property.MeshProperties;
import uk.nhs.prm.e2etests.exception.AuthorizationTokenException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Formatter;
import java.util.UUID;

@Component
public class AuthTokenGenerator {
    private static final String HMAC_SHA216 = "HmacSHA256";
    private static final String AUTHSCHEMANAME = "NHSMESH";
    private static final String env_shared = "BackBone";
    private final String mailboxId;
    private final String mailboxPassword;

    @Autowired
    public AuthTokenGenerator(MeshProperties meshProperties) {
        this.mailboxId = meshProperties.getMailboxId();
        this.mailboxPassword = meshProperties.getMailboxPassword();
    }

    private String toHexString(byte[] bytes) {
        Formatter formatter = new Formatter();
        for (byte b : bytes) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }

    private String calculateHMAC(String data) throws NoSuchAlgorithmException, InvalidKeyException {
       SecretKeySpec secretKeySpec = new SecretKeySpec(AuthTokenGenerator.env_shared.getBytes(), HMAC_SHA216);
        Mac mac = Mac.getInstance(HMAC_SHA216);
        mac.init(secretKeySpec);
        return toHexString(mac.doFinal(data.getBytes()));
    }

    public String getAuthorizationToken() {
        try {
            final String timeStamp = new SimpleDateFormat("yMdHms").format(Calendar.getInstance().getTime());
            final String nonce = generateNonce();
            final String nonce_count = "0";
            final String hmac_msg = this.mailboxId + ":" + nonce + ":" + nonce_count + ":" + this.mailboxPassword + ":" + timeStamp;
            final String hmac = calculateHMAC(hmac_msg);
            return AUTHSCHEMANAME + " " + this.mailboxId + ":" + nonce + ":" + nonce_count + ":" + timeStamp + ":" + hmac;
        } catch (NoSuchAlgorithmException | InvalidKeyException exception) {
            throw new AuthorizationTokenException(exception.getMessage());
        }
    }

    private String generateNonce() {
        return UUID.randomUUID().toString();
    }
}