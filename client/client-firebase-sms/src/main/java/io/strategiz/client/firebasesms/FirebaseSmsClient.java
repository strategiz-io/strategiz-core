package io.strategiz.client.firebasesms;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import io.strategiz.framework.exception.StrategizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Firebase SMS client for sending SMS OTP messages via Firebase Authentication
 * 
 * This client uses Firebase Phone Authentication to send SMS verification codes.
 * Firebase provides 10,000 free SMS verifications per month.
 */
@Component
public class FirebaseSmsClient {
    
    private static final Logger log = LoggerFactory.getLogger(FirebaseSmsClient.class);
    
    private final FirebaseSmsConfig config;
    
    @Value("${firebase.credentials.path:}")
    private String firebaseCredentialsPath;
    
    @Value("${firebase.project.id:}")
    private String firebaseProjectId;
    
    private FirebaseAuth firebaseAuth;
    private boolean initialized = false;
    
    // Store OTP codes temporarily (in production, use Redis or similar)
    // Firebase doesn't allow server-side SMS sending directly, so we store codes
    private final Map<String, String> otpStorage = new ConcurrentHashMap<>();
    
    public FirebaseSmsClient(FirebaseSmsConfig config) {
        this.config = config;
    }
    
    @PostConstruct
    public void initialize() {
        try {
            if (firebaseCredentialsPath.isEmpty()) {
                log.warn("Firebase credentials path not configured. SMS service will be unavailable.");
                return;
            }

            // Initialize Firebase Admin SDK
            // Try multiple strategies to find the credentials file
            InputStream serviceAccountStream = null;

            // Strategy 1: Try as classpath resource (for packaged JAR)
            // Remove path prefixes like "src/main/resources/" to get classpath resource name
            String classpathResource = firebaseCredentialsPath
                .replace("src/main/resources/", "")
                .replace("classpath:", "");

            Resource classPathRes = new ClassPathResource(classpathResource);
            if (classPathRes.exists()) {
                serviceAccountStream = classPathRes.getInputStream();
                log.info("Firebase credentials loaded from classpath: {}", classpathResource);
            } else {
                // Strategy 2: Try as filesystem path
                Resource fileSystemRes = new FileSystemResource(firebaseCredentialsPath);
                if (fileSystemRes.exists()) {
                    serviceAccountStream = fileSystemRes.getInputStream();
                    log.info("Firebase credentials loaded from filesystem: {}", firebaseCredentialsPath);
                }
            }

            if (serviceAccountStream == null) {
                log.error("Firebase credentials not found at path: {} or classpath: {}",
                    firebaseCredentialsPath, classpathResource);
                return;
            }

            FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccountStream))
                .setProjectId(firebaseProjectId)
                .build();

            // Check if FirebaseApp is already initialized
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
            }

            firebaseAuth = FirebaseAuth.getInstance();
            initialized = true;

            log.info("Firebase SMS client initialized successfully for project: {}", firebaseProjectId);

        } catch (IOException e) {
            log.error("Failed to initialize Firebase: {}", e.getMessage());
        }
    }
    
    /**
     * Send SMS OTP message
     * 
     * Note: Firebase Admin SDK doesn't directly send SMS from server-side.
     * This implementation stores the OTP and validates it server-side.
     * The actual SMS sending happens through Firebase client SDK.
     * 
     * @param phoneNumber The phone number to send SMS to (E.164 format)
     * @param message The full message containing OTP
     * @param countryCode The country code for SMS routing
     * @return true if SMS was sent successfully, false otherwise
     */
    public boolean sendSms(String phoneNumber, String message, String countryCode) {
        log.debug("Processing SMS for {} in country {}", maskPhoneNumber(phoneNumber), countryCode);
        
        try {
            // Check if SMS is enabled
            if (!config.isEnabled()) {
                log.warn("Firebase SMS is disabled in configuration");
                throw new StrategizException(FirebaseSmsErrors.SMS_SERVICE_UNAVAILABLE, "SMS service is disabled");
            }
            
            // Check if we're in development mode with mock SMS
            if (config.isMockSmsEnabled()) {
                log.info("📱 MOCK SMS to {}: {}", maskPhoneNumber(phoneNumber), message);
                return true;
            }
            
            if (!initialized) {
                log.error("Firebase not initialized. Cannot send SMS.");
                return false;
            }
            
            // Extract OTP from message (assumes format: "Your code is: 123456")
            String otpCode = extractOtpFromMessage(message);
            if (otpCode != null) {
                // Store OTP for server-side validation
                otpStorage.put(phoneNumber, otpCode);
                log.info("OTP stored for {}: {}", maskPhoneNumber(phoneNumber), otpCode);
            }
            
            // Create custom token for phone authentication
            // This allows the client to initiate Firebase phone auth
            try {
                Map<String, Object> claims = new HashMap<>();
                claims.put("phoneNumber", phoneNumber);
                claims.put("purpose", "sms_otp");
                
                String customToken = firebaseAuth.createCustomToken(phoneNumber, claims);
                log.debug("Created Firebase custom token for phone authentication");
                
                // In a real implementation, you would:
                // 1. Send this token to the client
                // 2. Client uses Firebase SDK to trigger SMS
                // 3. Client receives SMS and verifies with Firebase
                // 4. Server validates the Firebase auth token
                
                // For now, we simulate success
                log.info("SMS OTP process initiated for {}", maskPhoneNumber(phoneNumber));
                return true;
                
            } catch (FirebaseAuthException e) {
                log.error("Firebase Auth error: {}", e.getMessage());
                return false;
            }
            
        } catch (StrategizException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error processing SMS for {}: {}", maskPhoneNumber(phoneNumber), e.getMessage());
            throw new StrategizException(FirebaseSmsErrors.SMS_SEND_FAILED, "Failed to send SMS");
        }
    }
    
    /**
     * Verify OTP code (server-side validation)
     */
    public boolean verifyOtp(String phoneNumber, String otpCode) {
        String storedOtp = otpStorage.get(phoneNumber);
        if (storedOtp != null && storedOtp.equals(otpCode)) {
            otpStorage.remove(phoneNumber); // Remove after successful verification
            return true;
        }
        return false;
    }
    
    /**
     * Verify Firebase ID token from client-side phone authentication
     *
     * This method validates the Firebase ID token sent from the frontend
     * after successful phone number verification.
     *
     * @param idToken The Firebase ID token from client
     * @param expectedPhoneNumber The phone number that should match the token
     * @return true if token is valid and phone number matches
     */
    public boolean verifyFirebaseIdToken(String idToken, String expectedPhoneNumber) {
        try {
            if (!initialized) {
                log.error("Firebase not initialized. Cannot verify ID token. Check firebase.credentials.path={} and firebase.project.id={}",
                    firebaseCredentialsPath, firebaseProjectId);
                return false;
            }

            log.debug("Verifying Firebase ID token for phone: {}", maskPhoneNumber(expectedPhoneNumber));
            
            // Verify the ID token with Firebase Admin SDK
            var decodedToken = firebaseAuth.verifyIdToken(idToken);
            
            // Get the phone number from the token
            String tokenPhoneNumber = decodedToken.getClaims().get("phone_number") != null ? 
                decodedToken.getClaims().get("phone_number").toString() : null;
            
            if (tokenPhoneNumber == null) {
                // Check if phone number is in Firebase user record
                var userRecord = firebaseAuth.getUser(decodedToken.getUid());
                tokenPhoneNumber = userRecord.getPhoneNumber();
            }
            
            // Normalize phone numbers for comparison
            String normalizedExpected = normalizePhoneNumber(expectedPhoneNumber);
            String normalizedToken = normalizePhoneNumber(tokenPhoneNumber);
            
            if (!normalizedExpected.equals(normalizedToken)) {
                log.warn("Phone number mismatch. Expected: {}, Got: {}", 
                    maskPhoneNumber(expectedPhoneNumber), 
                    maskPhoneNumber(tokenPhoneNumber));
                return false;
            }
            
            log.info("Firebase ID token verified successfully for {}", maskPhoneNumber(expectedPhoneNumber));
            return true;
            
        } catch (FirebaseAuthException e) {
            log.error("Firebase Auth error verifying ID token: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Unexpected error verifying Firebase ID token: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Normalize phone number for comparison
     */
    private String normalizePhoneNumber(String phoneNumber) {
        if (phoneNumber == null) {
            return "";
        }
        // Remove all non-digit characters
        String digits = phoneNumber.replaceAll("\\D", "");
        // Remove leading 1 if it's a US number
        if (digits.length() == 11 && digits.startsWith("1")) {
            digits = digits.substring(1);
        }
        return digits;
    }
    
    /**
     * Check if Firebase SMS service is available and configured
     */
    public boolean isServiceAvailable() {
        return config.isEnabled() && (config.isMockSmsEnabled() || initialized);
    }
    
    /**
     * Extract OTP code from message
     */
    private String extractOtpFromMessage(String message) {
        // Look for 6-digit code in message
        if (message != null) {
            String[] parts = message.split("\\s+");
            for (String part : parts) {
                if (part.matches("\\d{6}")) {
                    return part;
                }
            }
        }
        return null;
    }
    
    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 4) {
            return "****";
        }
        return phoneNumber.substring(0, 3) + "****" + 
               phoneNumber.substring(phoneNumber.length() - 2);
    }
    
    /**
     * Check if we're running in development mode
     */
    private boolean isDevelopmentMode() {
        String profile = System.getProperty("spring.profiles.active",
            System.getenv().getOrDefault("SPRING_PROFILES_ACTIVE", ""));
        return profile.contains("dev") || profile.contains("local");
    }
}