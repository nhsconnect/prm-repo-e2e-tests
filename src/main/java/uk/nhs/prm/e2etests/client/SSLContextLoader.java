package uk.nhs.prm.e2etests.client;

import jakarta.xml.bind.DatatypeConverter;
import lombok.extern.log4j.Log4j2;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.springframework.stereotype.Component;
import uk.nhs.prm.e2etests.exception.SSLContextException;

import javax.net.ssl.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

@Log4j2
@Component
public class SSLContextLoader {
    public SSLContext getClientAuthSslContext(String clientCertInPemFormat, String clientKeyInPemFormat){
        log.info("Attempting to initialise the SSL context so that we can make requests to the Mesh Mailbox.");
        SSLContext sslContext = null;

        try {
            sslContext = SSLContext.getInstance("TLS");
            byte[] certDerBytes = parseDERFromPEM(clientCertInPemFormat.getBytes(), "-----BEGIN CERTIFICATE-----", "-----END CERTIFICATE-----");

            X509Certificate cert = generateCertificateFromDER(certDerBytes);
            RSAPrivateKey key = getRsaPrivateKey(clientKeyInPemFormat);

            KeyStore keystore = KeyStore.getInstance("JKS");
            keystore.load(null);
            keystore.setCertificateEntry("client-cert", cert);
            keystore.setKeyEntry("client-key", key, "password".toCharArray(), new Certificate[]{ cert });

            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(keystore, "password".toCharArray());

            KeyManager[] km = kmf.getKeyManagers();

            sslContext.init(km, null, null);
            TrustManager acceptAll = new X509TrustManager() {
                public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                }

                public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                }

                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
            };

            sslContext.init(km, new TrustManager[]{acceptAll}, null);
            log.info("The SSL Context has successfully been initialised.");

        } catch (Exception exception){
            throw new SSLContextException(exception.getMessage());
        }

        return sslContext;
    }

    private RSAPrivateKey getRsaPrivateKey(String clientKeyInPemFormat) throws IOException, InvalidKeySpecException, NoSuchAlgorithmException {
        if (clientKeyInPemFormat.startsWith("-----BEGIN RSA PRIVATE KEY-----")) {
            return getPrivateKeyForRSAKeyString(clientKeyInPemFormat);
        } else {
            byte[] keyDerBytes = parseDERFromPEM(clientKeyInPemFormat.getBytes(), "-----BEGIN PRIVATE KEY-----", "-----END PRIVATE KEY-----");
            return generatePrivateKeyFromDER(keyDerBytes);
        }
    }

    private static byte[] parseDERFromPEM(byte[] pem, String beginDelimiter, String endDelimiter) {
        String data = new String(pem);
        String[] tokens = data.split(beginDelimiter);
        tokens = tokens[1].split(endDelimiter);
        return DatatypeConverter.parseBase64Binary(tokens[0]);
    }

    private RSAPrivateKey getPrivateKeyForRSAKeyString(String privateKeyString) throws IOException {
        PEMParser pemParser = new PEMParser(new StringReader(privateKeyString));
        Security.addProvider(new BouncyCastleProvider());
        JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");
        Object object = pemParser.readObject();
        KeyPair kp = converter.getKeyPair((PEMKeyPair) object);
        return (RSAPrivateKey) kp.getPrivate();
    }

    private RSAPrivateKey generatePrivateKeyFromDER(byte[] keyBytes) throws InvalidKeySpecException, NoSuchAlgorithmException {
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory factory = KeyFactory.getInstance("RSA");
        return (RSAPrivateKey) factory.generatePrivate(spec);
    }

    private X509Certificate generateCertificateFromDER(byte[] certificateBytes) throws CertificateException {
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        return (X509Certificate) factory.generateCertificate(new ByteArrayInputStream(certificateBytes));
    }
}
