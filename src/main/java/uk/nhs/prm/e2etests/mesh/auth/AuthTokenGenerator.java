package uk.nhs.prm.e2etests.mesh.auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.nhs.prm.e2etests.annotation.Debt;
import uk.nhs.prm.e2etests.exception.AuthorizationTokenException;
import uk.nhs.prm.e2etests.property.MeshProperties;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Formatter;
import java.util.IllegalFormatException;
import java.util.UUID;

import static uk.nhs.prm.e2etests.annotation.Debt.Priority.HIGH;

@Component
public class AuthTokenGenerator {
    private static final String HMAC_SHA216_ALGORITHM_NAME = "HmacSHA256";
    @Debt(comment = "Abstract this out to an AWS service such as parameter store or SSM.", priority = HIGH)
    private static final String AUTHORISATION_SCHEMA_NAME = "NHSMESH";
    @Debt(comment = "Abstract this out to an AWS service such as parameter store or SSM.", priority = HIGH)
    private static final String SHARED_ENVIRONMENT_NAME = "BackBone";
    private final String mailboxId;
    private final String mailboxPassword;

    @Autowired
    public AuthTokenGenerator(
            MeshProperties meshProperties
    ) {
        this.mailboxId = meshProperties.getMailboxId();
        this.mailboxPassword = meshProperties.getMailboxPassword();
    }

    private String toHexadecimalString(byte[] bytes) {
        try(Formatter formatter = new Formatter()) {
            for (byte currentByte : bytes) formatter.format("%02x", currentByte);
            return formatter.toString();
        } catch (IllegalFormatException exception) {
            throw new AuthorizationTokenException(exception.getMessage());
        }
    }

    public String getAuthorizationToken() {
        try {
            final String timeStamp = new SimpleDateFormat("yMdHms").format(Calendar.getInstance().getTime());
            final String nonce = UUID.randomUUID().toString();
            final String nonce_count = "0";
            final String hmac_msg = this.mailboxId + ":" + nonce + ":" + nonce_count + ":" + this.mailboxPassword + ":" + timeStamp;
            final String hmac = calculateHMAC(hmac_msg);
            return AUTHORISATION_SCHEMA_NAME + " " + this.mailboxId + ":" + nonce + ":" + nonce_count + ":" + timeStamp + ":" + hmac;
        } catch (NoSuchAlgorithmException | InvalidKeyException exception) {
            throw new AuthorizationTokenException(exception.getMessage());
        }
    }

    private String calculateHMAC(String data) throws NoSuchAlgorithmException, InvalidKeyException {
        SecretKeySpec secretKeySpec =
                new SecretKeySpec(AuthTokenGenerator.SHARED_ENVIRONMENT_NAME.getBytes(), HMAC_SHA216_ALGORITHM_NAME);
        Mac messageAuthenticationCode = Mac.getInstance(HMAC_SHA216_ALGORITHM_NAME);
        messageAuthenticationCode.init(secretKeySpec);
        return toHexadecimalString(messageAuthenticationCode.doFinal(data.getBytes()));
    }
}