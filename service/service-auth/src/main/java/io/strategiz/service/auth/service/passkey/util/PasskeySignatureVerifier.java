package io.strategiz.service.auth.service.passkey.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.webauthn4j.converter.util.ObjectConverter;
import com.webauthn4j.data.attestation.authenticator.COSEKey;
import com.webauthn4j.data.attestation.authenticator.EC2COSEKey;
import com.webauthn4j.data.attestation.authenticator.RSACOSEKey;
import com.webauthn4j.util.Base64UrlUtil;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.ECPoint;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidParameterSpecException;
import java.security.AlgorithmParameters;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Base64;
import java.security.interfaces.ECPublicKey;

/**
 * Utility class for WebAuthn signature verification operations
 */
public class PasskeySignatureVerifier {
    private static final Logger log = LoggerFactory.getLogger(PasskeySignatureVerifier.class);
    
    private static final ObjectConverter objectConverter = new ObjectConverter();
    
    private PasskeySignatureVerifier() {
        // Private constructor to prevent instantiation
    }
    
    /**
     * Verify WebAuthn assertion signature with detailed logging
     * 
     * @param publicKeyBase64 Base64 encoded COSE key
     * @param authenticatorData Raw authenticator data (Base64 URL)
     * @param clientDataJSON Client data JSON  
     * @param signatureBase64 Base64 URL encoded signature
     * @return True if signature is valid
     */
    public static boolean verifySignature(
            String publicKeyBase64, 
            String authenticatorData, 
            String clientDataJSON, 
            String signatureBase64) {
        
        try {
            log.info("=== Starting signature verification ===");
            log.info("Public key (first 50 chars): {}", publicKeyBase64.substring(0, Math.min(50, publicKeyBase64.length())));
            log.info("Authenticator data length: {}", authenticatorData.length());
            log.info("Client data JSON length: {}", clientDataJSON.length());
            log.info("Signature length: {}", signatureBase64.length());
            
            // Debug Base64 URL decoding
            log.info("Raw authenticator data (first 50 chars): {}", authenticatorData.substring(0, Math.min(50, authenticatorData.length())));
            log.info("Raw signature (first 50 chars): {}", signatureBase64.substring(0, Math.min(50, signatureBase64.length())));
            
            // Debug client data JSON content
            log.info("Client data JSON content: {}", clientDataJSON);
            
            // Decode client data JSON from Base64 if needed
            String actualClientDataJSON;
            try {
                // Try to decode from Base64 URL
                byte[] decodedBytes = Base64.getUrlDecoder().decode(clientDataJSON);
                actualClientDataJSON = new String(decodedBytes, StandardCharsets.UTF_8);
                log.info("Decoded client data JSON: {}", actualClientDataJSON);
            } catch (Exception e) {
                // If decoding fails, assume it's already plain JSON
                actualClientDataJSON = clientDataJSON;
                log.info("Client data JSON already decoded (or decode failed): {}", actualClientDataJSON);
            }
            
            // Parse the COSE key
            byte[] coseKeyBytes = Base64.getDecoder().decode(publicKeyBase64);
            COSEKey coseKey = objectConverter.getCborConverter().readValue(coseKeyBytes, COSEKey.class);
            log.info("Successfully parsed COSE key: {}", coseKey.getClass().getSimpleName());
            
            // Convert COSE key to Java PublicKey
            PublicKey publicKey = convertCoseKeyToPublicKey(coseKey);
            if (publicKey == null) {
                log.error("Failed to convert COSE key to PublicKey");
                return false;
            }
            log.info("Successfully converted to PublicKey algorithm: {}", publicKey.getAlgorithm());
            
            // Parse signature
            byte[] signatureBytes = Base64UrlUtil.decode(signatureBase64);
            log.info("Parsed signature bytes length: {}", signatureBytes.length);
            
            // Parse authenticator data
            byte[] authenticatorDataBytes = Base64UrlUtil.decode(authenticatorData);
            log.info("Parsed authenticator data bytes length: {}", authenticatorDataBytes.length);
            
            // Hash client data
            byte[] clientDataHash = hashClientData(actualClientDataJSON);
            log.info("Client data hash length: {}", clientDataHash.length);
            
            // Create verification data
            byte[] verificationData = createVerificationData(authenticatorDataBytes, clientDataHash);
            log.info("Verification data length: {}", verificationData.length);
            
            // Determine signature algorithm
            String signatureAlgorithm = determineSignatureAlgorithm(coseKey);
            log.info("Using signature algorithm: {}", signatureAlgorithm);
            
            // Debug signature format
            log.info("Signature bytes (hex): {}", bytesToHex(signatureBytes));
            log.info("Signature first 4 bytes: {}", Arrays.toString(Arrays.copyOf(signatureBytes, Math.min(4, signatureBytes.length))));
            
            // Debug verification data
            log.info("Verification data (hex): {}", bytesToHex(verificationData));
            log.info("Authenticator data (hex): {}", bytesToHex(authenticatorDataBytes));
            log.info("Client data hash (hex): {}", bytesToHex(clientDataHash));
            
            // Debug the exact verification data construction
            log.info("Verification data construction check:");
            log.info("  - Authenticator data offset: 0 to {}", authenticatorDataBytes.length - 1);
            log.info("  - Client data hash offset: {} to {}", authenticatorDataBytes.length, verificationData.length - 1);
            log.info("  - Total verification data length: {}", verificationData.length);
            
            // Verify the concatenation is correct
            byte[] manualConcat = new byte[authenticatorDataBytes.length + clientDataHash.length];
            System.arraycopy(authenticatorDataBytes, 0, manualConcat, 0, authenticatorDataBytes.length);
            System.arraycopy(clientDataHash, 0, manualConcat, authenticatorDataBytes.length, clientDataHash.length);
            boolean concatMatches = Arrays.equals(verificationData, manualConcat);
            log.info("Manual concatenation matches createVerificationData: {}", concatMatches);
            
            // Debug public key
            log.info("Public key format: {}", publicKey.getFormat());
            log.info("Public key algorithm: {}", publicKey.getAlgorithm());
            log.info("Public key encoded length: {}", publicKey.getEncoded().length);
            
            // Debug the actual public key bytes
            log.info("Public key bytes (hex): {}", bytesToHex(publicKey.getEncoded()));
            
            // Try to verify the EC curve parameters
            if (publicKey instanceof ECPublicKey) {
                ECPublicKey ecPublicKey = (ECPublicKey) publicKey;
                ECParameterSpec params = ecPublicKey.getParams();
                log.info("EC curve name: {}", params.getCurve().toString());
                log.info("EC field size: {}", params.getCurve().getField().getFieldSize());
                ECPoint point = ecPublicKey.getW();
                log.info("EC point X from PublicKey: {}", point.getAffineX().toString(16));
                log.info("EC point Y from PublicKey: {}", point.getAffineY().toString(16));
            }
            
            // Verify signature (Java expects DER format for ECDSA)
            Signature signature = Signature.getInstance(signatureAlgorithm);
            signature.initVerify(publicKey);
            signature.update(verificationData);
            
            boolean isValid = signature.verify(signatureBytes);
            log.info("=== Signature verification result: {} ===", isValid);
            
            // If verification failed, try alternative approach
            if (!isValid) {
                log.info("=== Attempting alternative signature verification ===");
                try {
                    // Try with different provider or approach if available
                    Signature altSignature = Signature.getInstance("SHA256withECDSA", "SunEC");
                    altSignature.initVerify(publicKey);
                    altSignature.update(verificationData);
                    boolean altValid = altSignature.verify(signatureBytes);
                    log.info("Alternative signature verification result: {}", altValid);
                    if (altValid) {
                        isValid = true;
                    }
                } catch (Exception e) {
                    log.warn("Alternative signature verification failed: {}", e.getMessage());
                }
            }
            
            return isValid;
            
        } catch (Exception e) {
            log.error("Signature verification failed with error: {}", e.getMessage());
            log.error("Full stack trace:", e);
            return false;
        }
    }
    
    /**
     * Convert COSE key to Java PublicKey
     */
    private static PublicKey convertCoseKeyToPublicKey(COSEKey coseKey) 
            throws InvalidKeySpecException, NoSuchAlgorithmException, InvalidParameterSpecException {
        
        if (coseKey instanceof EC2COSEKey) {
            EC2COSEKey ec2Key = (EC2COSEKey) coseKey;
            log.info("Processing EC2 COSE key");
            
            // Get curve parameters for P-256 (secp256r1) - same method as main verification
            AlgorithmParameters parameters = AlgorithmParameters.getInstance("EC");
            parameters.init(new ECGenParameterSpec("secp256r1"));
            ECParameterSpec ecParameterSpec = parameters.getParameterSpec(ECParameterSpec.class);
            
            // Create EC point from X and Y coordinates
            BigInteger x = new BigInteger(1, ec2Key.getX());
            BigInteger y = new BigInteger(1, ec2Key.getY());
            ECPoint ecPoint = new ECPoint(x, y);
            
            log.info("EC Point X coordinate length: {}", ec2Key.getX().length);
            log.info("EC Point Y coordinate length: {}", ec2Key.getY().length);
            
            // Debug the actual coordinate values
            log.info("EC Point X coordinate (hex): {}", bytesToHex(ec2Key.getX()));
            log.info("EC Point Y coordinate (hex): {}", bytesToHex(ec2Key.getY()));
            
            // Create EC public key spec
            ECPublicKeySpec keySpec = new ECPublicKeySpec(ecPoint, ecParameterSpec);
            
            // Generate public key
            KeyFactory keyFactory = KeyFactory.getInstance("EC");
            return keyFactory.generatePublic(keySpec);
            
        } else if (coseKey instanceof RSACOSEKey) {
            RSACOSEKey rsaKey = (RSACOSEKey) coseKey;
            log.info("Processing RSA COSE key");
            
            // Create RSA public key spec
            BigInteger n = new BigInteger(1, rsaKey.getN());
            BigInteger e = new BigInteger(1, rsaKey.getE());
            RSAPublicKeySpec keySpec = new RSAPublicKeySpec(n, e);
            
            // Generate public key
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePublic(keySpec);
            
        } else {
            log.error("Unsupported COSE key type: {}", coseKey.getClass().getSimpleName());
            return null;
        }
    }
    
    /**
     * Determine signature algorithm based on COSE key type
     */
    private static String determineSignatureAlgorithm(COSEKey coseKey) {
        if (coseKey instanceof EC2COSEKey) {
            return "SHA256withECDSA";
        } else if (coseKey instanceof RSACOSEKey) {
            return "SHA256withRSA";
        } else {
            log.warn("Unknown COSE key type, defaulting to SHA256withECDSA");
            return "SHA256withECDSA";
        }
    }
    
    /**
     * Create verification data by concatenating authenticator data and client data hash
     */
    private static byte[] createVerificationData(byte[] authenticatorData, byte[] clientDataHash) {
        ByteBuffer buffer = ByteBuffer.allocate(authenticatorData.length + clientDataHash.length);
        buffer.put(authenticatorData);
        buffer.put(clientDataHash);
        return buffer.array();
    }
    
    /**
     * Hash client data JSON using SHA-256
     */
    private static byte[] hashClientData(String clientDataJSON) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(clientDataJSON.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            log.error("Failed to hash client data", e);
            throw new RuntimeException("Failed to hash client data", e);
        }
    }
    
    /**
     * Check if signature is DER-encoded (starts with 0x30)
     */
    private static boolean isDerEncodedSignature(byte[] signature) {
        return signature != null && signature.length > 0 && (signature[0] & 0xFF) == 0x30;
    }
    
    /**
     * Convert DER-encoded ECDSA signature to raw format (r,s values)
     */
    private static byte[] convertDerToRawEcdsa(byte[] derSignature) {
        try {
            // Parse DER structure: SEQUENCE { r INTEGER, s INTEGER }
            if (derSignature[0] != 0x30) {
                throw new IllegalArgumentException("Not a valid DER sequence");
            }
            
            int offset = 2; // Skip SEQUENCE tag and length
            
            // Parse r value
            if (derSignature[offset] != 0x02) {
                throw new IllegalArgumentException("Expected INTEGER tag for r");
            }
            offset++; // Skip INTEGER tag
            int rLength = derSignature[offset] & 0xFF;
            offset++; // Skip length byte
            
            byte[] r = new byte[rLength];
            System.arraycopy(derSignature, offset, r, 0, rLength);
            offset += rLength;
            
            // Parse s value
            if (derSignature[offset] != 0x02) {
                throw new IllegalArgumentException("Expected INTEGER tag for s");
            }
            offset++; // Skip INTEGER tag
            int sLength = derSignature[offset] & 0xFF;
            offset++; // Skip length byte
            
            byte[] s = new byte[sLength];
            System.arraycopy(derSignature, offset, s, 0, sLength);
            
            // Remove leading zeros from r and s if present
            r = removeLeadingZeros(r);
            s = removeLeadingZeros(s);
            
            // For P-256, both r and s should be 32 bytes
            r = padToLength(r, 32);
            s = padToLength(s, 32);
            
            // Concatenate r and s
            byte[] rawSignature = new byte[64];
            System.arraycopy(r, 0, rawSignature, 0, 32);
            System.arraycopy(s, 0, rawSignature, 32, 32);
            
            log.info("DER to raw conversion: DER length {} -> raw length {}", derSignature.length, rawSignature.length);
            return rawSignature;
            
        } catch (Exception e) {
            log.error("Failed to convert DER signature to raw format", e);
            // Return original signature if conversion fails
            return derSignature;
        }
    }
    
    /**
     * Remove leading zero bytes from byte array
     */
    private static byte[] removeLeadingZeros(byte[] bytes) {
        int firstNonZero = 0;
        while (firstNonZero < bytes.length && bytes[firstNonZero] == 0) {
            firstNonZero++;
        }
        
        if (firstNonZero == 0) {
            return bytes;
        }
        
        byte[] result = new byte[bytes.length - firstNonZero];
        System.arraycopy(bytes, firstNonZero, result, 0, result.length);
        return result;
    }
    
    /**
     * Pad byte array to specified length with leading zeros
     */
    private static byte[] padToLength(byte[] bytes, int targetLength) {
        if (bytes.length >= targetLength) {
            return bytes;
        }
        
        byte[] padded = new byte[targetLength];
        System.arraycopy(bytes, 0, padded, targetLength - bytes.length, bytes.length);
        return padded;
    }
    
    /**
     * Convert byte array to hex string for debugging
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}
