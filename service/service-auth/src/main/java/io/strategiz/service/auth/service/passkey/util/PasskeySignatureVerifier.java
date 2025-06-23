package io.strategiz.service.auth.service.passkey.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Utility class for WebAuthn signature verification operations
 */
public class PasskeySignatureVerifier {
    private static final Logger log = LoggerFactory.getLogger(PasskeySignatureVerifier.class);
    
    private PasskeySignatureVerifier() {
        // Private constructor to prevent instantiation
    }
    
    /**
     * Verify WebAuthn assertion signature
     * 
     * @param publicKeyBase64 Base64 URL encoded public key
     * @param authenticatorData Raw authenticator data
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
            // Decode the public key
            byte[] publicKeyBytes = Base64.getUrlDecoder().decode(publicKeyBase64);
            PublicKey publicKey = KeyFactory.getInstance("EC")
                    .generatePublic(new X509EncodedKeySpec(publicKeyBytes));
            
            // Decode signature
            byte[] signatureBytes = Base64.getUrlDecoder().decode(signatureBase64);
            
            // Decode authenticator data
            byte[] authenticatorDataBytes = Base64.getUrlDecoder().decode(authenticatorData);
            
            // Hash the client data JSON
            byte[] clientDataHash = hashClientData(clientDataJSON);
            
            // Create verification data
            byte[] verificationData = createVerificationData(authenticatorDataBytes, clientDataHash);
            
            // Verify signature
            Signature ecdsaSignature = Signature.getInstance("SHA256withECDSA");
            ecdsaSignature.initVerify(publicKey);
            ecdsaSignature.update(verificationData);
            
            return ecdsaSignature.verify(signatureBytes);
            
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | SignatureException e) {
            log.error("Error verifying WebAuthn signature", e);
            return false;
        } catch (Exception e) {
            log.error("Unexpected error during signature verification", e);
            return false;
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
}
