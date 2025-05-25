package io.strategiz.service.exchange.coinbase.util;

import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.sec.ECPrivateKey;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x9.X9ObjectIdentifiers;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.bouncycastle.util.io.pem.PemWriter;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Base64;

/**
 * Utility class for PEM key operations
 */
public class PemKeyUtil {
    /**
     * Converts a SEC1 EC private key PEM (-----BEGIN EC PRIVATE KEY-----) to PKCS#8 PEM (-----BEGIN PRIVATE KEY-----)
     * If already PKCS#8, returns as-is.
     */
    public static String ensurePkcs8Pem(String pem) throws IOException {
        if (pem.contains("BEGIN PRIVATE KEY")) {
            // Already PKCS#8
            return pem;
        }
        if (!pem.contains("BEGIN EC PRIVATE KEY")) {
            throw new IllegalArgumentException("Unknown PEM format");
        }
        try (PemReader pemReader = new PemReader(new StringReader(pem))) {
            PemObject pemObject = pemReader.readPemObject();
            ASN1Primitive asn1 = ASN1Primitive.fromByteArray(pemObject.getContent());
            ECPrivateKey ecPrivateKey = ECPrivateKey.getInstance(asn1);
            AlgorithmIdentifier algId = new AlgorithmIdentifier(X9ObjectIdentifiers.id_ecPublicKey, ecPrivateKey.getParameters());
            PrivateKeyInfo pkInfo = new PrivateKeyInfo(algId, ecPrivateKey);
            byte[] pkcs8Bytes = pkInfo.getEncoded();
            StringWriter sw = new StringWriter();
            try (PemWriter pemWriter = new PemWriter(sw)) {
                pemWriter.writeObject(new PemObject("PRIVATE KEY", pkcs8Bytes));
            }
            return sw.toString();
        }
    }
}
