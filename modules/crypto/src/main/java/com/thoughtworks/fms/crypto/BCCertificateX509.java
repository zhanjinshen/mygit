package com.thoughtworks.fms.crypto;

import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.joda.time.DateTime;

import java.math.BigInteger;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;

import static org.bouncycastle.jce.provider.BouncyCastleProvider.PROVIDER_NAME;

public final class BCCertificateX509 {

    private BCCertificateX509() {
    }

    public static Certificate generateCertificate(KeyPair keyPair) throws CertificateException {
        try {
            Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

            X500NameBuilder builder = new X500NameBuilder(BCStyle.INSTANCE);
            builder.addRDN(BCStyle.OU, "P2P");
            builder.addRDN(BCStyle.O, "JIASHI");
            builder.addRDN(BCStyle.CN, "localhost");

            Date notBefore = new DateTime().minusYears(1).toDate();
            Date notAfter = new DateTime().plusYears(10).toDate();
            BigInteger serial = BigInteger.valueOf(System.currentTimeMillis());

            X509v3CertificateBuilder certGen = new JcaX509v3CertificateBuilder(builder.build(),
                    serial, notBefore, notAfter, builder.build(), keyPair.getPublic());

            ContentSigner sigGen = new JcaContentSignerBuilder("SHA256WithRSAEncryption")
                    .setProvider(PROVIDER_NAME).build(keyPair.getPrivate());

            X509Certificate cert = new JcaX509CertificateConverter().setProvider(PROVIDER_NAME)
                    .getCertificate(certGen.build(sigGen));

            cert.checkValidity(new Date());
            cert.verify(cert.getPublicKey());

            return cert;
        } catch (OperatorCreationException | NoSuchProviderException | NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            throw new CertificateException(e);
        }
    }
}
