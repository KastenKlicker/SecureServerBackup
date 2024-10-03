package de.kastenklicker.secureserverbackup;

import org.testcontainers.shaded.org.bouncycastle.asn1.x500.X500Name;
import org.testcontainers.shaded.org.bouncycastle.asn1.x509.*;
import org.testcontainers.shaded.org.bouncycastle.cert.X509v3CertificateBuilder;
import org.testcontainers.shaded.org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.testcontainers.shaded.org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.testcontainers.shaded.org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.testcontainers.shaded.org.bouncycastle.operator.ContentSigner;
import org.testcontainers.shaded.org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;

public class CertGenerator {
    
    public static void generate(File keyFile, File certFile) throws Exception {
        // Schlüsselpaar (RSA) generieren
        java.security.KeyPairGenerator keyPairGenerator = java.security.KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048, new SecureRandom());
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        PrivateKey privateKey = keyPair.getPrivate();

        // Gültigkeitszeitraum setzen
        Calendar calendar = Calendar.getInstance();
        Date startDate = calendar.getTime();  // jetzt
        // TODO to 1 day
        calendar.add(Calendar.YEAR, 1);       // Gültigkeit: 1 Jahr
        Date endDate = calendar.getTime();

        // Zertifikatsinformationen festlegen (DN, Seriennummer, Algorithmus, etc.)
        X500Name dnName = new X500Name("CN=localhost");
        BigInteger certSerialNumber = new BigInteger(Long.toString(new SecureRandom().nextLong()));
        X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                dnName, certSerialNumber, startDate, endDate, dnName, keyPair.getPublic());

        // Erweiterungen hinzufügen (KeyUsage, SAN, etc.)
        certBuilder.addExtension(
                Extension.subjectAlternativeName, false,
                new GeneralNames(new GeneralName(GeneralName.dNSName, "localhost")));
        certBuilder.addExtension(
                Extension.keyUsage, true, new KeyUsage(KeyUsage.digitalSignature));
        certBuilder.addExtension(
                Extension.extendedKeyUsage, false,
                new ExtendedKeyUsage(KeyPurposeId.id_kp_serverAuth));

        // Zertifikat mit SHA256 und dem privaten Schlüssel signieren
        ContentSigner contentSigner = new JcaContentSignerBuilder("SHA256WithRSA").build(privateKey);
        X509Certificate certificate = new JcaX509CertificateConverter().getCertificate(certBuilder.build(contentSigner));

        // PEM-Dateien schreiben        
        try (Writer certWriter = new FileWriter(certFile);
             JcaPEMWriter pemCertWriter = new JcaPEMWriter(certWriter)) {
            pemCertWriter.writeObject(certificate);
        }

        try (Writer keyWriter = new FileWriter(keyFile);
             JcaPEMWriter pemKeyWriter = new JcaPEMWriter(keyWriter)) {
            pemKeyWriter.writeObject(privateKey);
        }
    }
}
