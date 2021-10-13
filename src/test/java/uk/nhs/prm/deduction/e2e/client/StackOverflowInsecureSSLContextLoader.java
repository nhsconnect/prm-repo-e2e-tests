package uk.nhs.prm.deduction.e2e.client;

import javax.net.ssl.*;
import javax.xml.bind.DatatypeConverter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

public class StackOverflowInsecureSSLContextLoader {


    public SSLContext getClientAuthSslContext(String clientCertInPemFormat, String clientKeyInPemFormat){

        log("** initialising SSL context to make request to Mesh Mailbox");
        SSLContext sslContext = null;
        try {
            sslContext = SSLContext.getInstance("TLS");
            byte[] certDerBytes = parseDERFromPEM(clientCertInPemFormat.getBytes(), "-----BEGIN CERTIFICATE-----", "-----END CERTIFICATE-----");
            byte[] keyDerBytes = parseDERFromPEM(clientKeyInPemFormat.getBytes(), "-----BEGIN PRIVATE KEY-----", "-----END PRIVATE KEY-----");

            X509Certificate cert = generateCertificateFromDER(certDerBytes);
            RSAPrivateKey key = generatePrivateKeyFromDER(keyDerBytes);

            KeyStore keystore = KeyStore.getInstance("JKS");
            keystore.load(null);
            keystore.setCertificateEntry("client-cert", cert);
            keystore.setKeyEntry("client-key", key, "password".toCharArray(), new Certificate[]{cert});

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

            log("** SSL context initialised");

        }catch (Exception e){
            log("** encountered exception %s",e.getMessage());
            e.printStackTrace();
        }
        return sslContext;
    }

    private static byte[] parseDERFromPEM(byte[] pem, String beginDelimiter, String endDelimiter) {
        String data = new String(pem);
        String[] tokens = data.split(beginDelimiter);
        tokens = tokens[1].split(endDelimiter);
        return DatatypeConverter.parseBase64Binary(tokens[0]);
    }

    private static RSAPrivateKey generatePrivateKeyFromDER(byte[] keyBytes) throws InvalidKeySpecException, NoSuchAlgorithmException {
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);

        KeyFactory factory = KeyFactory.getInstance("RSA");

        return (RSAPrivateKey) factory.generatePrivate(spec);
    }

    private static X509Certificate generateCertificateFromDER(byte[] certBytes) throws CertificateException {
        CertificateFactory factory = CertificateFactory.getInstance("X.509");

        return (X509Certificate) factory.generateCertificate(new ByteArrayInputStream(certBytes));
    }


    public void log(String message) {
        System.out.println(message);
    }

    public void log(String messageBody, String messageValue) {
        System.out.println(String.format(messageBody, messageValue));
    }
}
