package io.strategiz.service.auth.service.token;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.strategiz.service.auth.model.token.SignupIdentityToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

/**
 * Service for creating and validating signup identity tokens
 * These tokens track identity throughout the multi-step signup process
 */
@Service
public class SignupTokenService {
    
    private static final Logger log = LoggerFactory.getLogger(SignupTokenService.class);
    
    @Autowired
    private ObjectMapper objectMapper;
    
    // Simple encryption key for development purposes
    private final SecretKey encryptionKey;
    
    /**
     * Constructor initializes a secret key for token encryption
     */
    public SignupTokenService() {
        try {
            // In a real app, this would be loaded from a secure key store
            // For development, we generate a new key on startup
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(256, new SecureRandom());
            this.encryptionKey = keyGen.generateKey();
            log.info("Initialized signup token service with temporary key");
        } catch (Exception e) {
            log.error("Failed to initialize encryption key", e);
            throw new RuntimeException("Failed to initialize token service", e);
        }
    }

    /**
     * Create a new signup identity token after email verification
     * 
     * @param email User's verified email
     * @param displayName User's display name (if provided)
     * @return Encrypted token string
     */
    public String createSignupToken(String email, String displayName) {
        String verificationId = UUID.randomUUID().toString();
        SignupIdentityToken tokenPayload = SignupIdentityToken.create(email, verificationId, displayName);
        
        try {
            String json = objectMapper.writeValueAsString(tokenPayload);
            
            // Encrypt the JSON data
            byte[] encryptedData = encrypt(json.getBytes(StandardCharsets.UTF_8));
            
            // Base64 encode for transport
            return Base64.getEncoder().encodeToString(encryptedData);
        } catch (Exception e) {
            log.error("Error creating signup token", e);
            throw new RuntimeException("Error creating signup token", e);
        }
    }
    
    /**
     * Validate and extract the signup identity token
     * 
     * @param token The encrypted token string
     * @return Decoded SignupIdentityToken or null if invalid
     */
    public SignupIdentityToken validateSignupToken(String token) {
        try {
            // Base64 decode the token
            byte[] encryptedData = Base64.getDecoder().decode(token);
            
            // Decrypt the data
            byte[] jsonBytes = decrypt(encryptedData);
            
            // Parse the JSON data
            SignupIdentityToken identityToken = objectMapper.readValue(
                    new String(jsonBytes, StandardCharsets.UTF_8), SignupIdentityToken.class);
            
            if (identityToken.isExpired()) {
                log.warn("Signup token expired for email: {}", identityToken.email());
                return null;
            }
            
            return identityToken;
        } catch (Exception e) {
            log.error("Error validating signup token", e);
            return null;
        }
    }
    
    /**
     * Encrypt data using AES
     * 
     * @param data Data to encrypt
     * @return Encrypted data
     */
    private byte[] encrypt(byte[] data) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, encryptionKey);
        return cipher.doFinal(data);
    }
    
    /**
     * Decrypt data using AES
     * 
     * @param encryptedData Data to decrypt
     * @return Decrypted data
     */
    private byte[] decrypt(byte[] encryptedData) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, encryptionKey);
        return cipher.doFinal(encryptedData);
    }
}
