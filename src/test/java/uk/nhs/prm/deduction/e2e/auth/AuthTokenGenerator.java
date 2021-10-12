package uk.nhs.prm.deduction.e2e.auth;

import uk.nhs.prm.deduction.e2e.TestConfiguration;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Formatter;
import java.util.UUID;

public class AuthTokenGenerator {
    private static final String HMAC_SHA216 = "HmacSHA256";
    private static final String AUTHSCHEMANAME = "NHSMESH";
    private static final String env_shared_secret = "BackBone";
    private static final String nonce = UUID.randomUUID().toString();
    private static final String nonce_count = "0";
    TestConfiguration configuration;
    public AuthTokenGenerator(TestConfiguration configuration) {
        this.configuration = configuration;
    }

    private String toHexString(byte[] bytes) {
        Formatter formatter = new Formatter();
        for (byte b : bytes) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }

    private String calculateHMAC(String data, String key)
            throws NoSuchAlgorithmException, InvalidKeyException
    {
       SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(), HMAC_SHA216);
        Mac mac = Mac.getInstance(HMAC_SHA216);
        mac.init(secretKeySpec);
        return toHexString(mac.doFinal(data.getBytes()));
    }

    public String getAuthorizationToken() throws Exception {
        String timeStamp = new SimpleDateFormat("YmdHMS").format(Calendar.getInstance().getTime());
        String hmac_msg = configuration.getMeshMailBoxID() + ":" + nonce + ":" + nonce_count  + ":" + configuration.getMeshMailBoxPassword() + ":" + timeStamp;
        String hmac = calculateHMAC(hmac_msg, env_shared_secret);
        String s = AUTHSCHEMANAME+" "+ configuration.getMeshMailBoxID() + ":" + nonce + ":"+nonce_count + ":" + timeStamp+ ":"+ hmac;
        return s;
    }

}
