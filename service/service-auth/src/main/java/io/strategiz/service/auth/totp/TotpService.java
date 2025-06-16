package io.strategiz.service.auth.totp;

import dev.samstevens.totp.code.*;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import io.strategiz.business.tokenauth.SessionAuthBusiness;
import io.strategiz.data.user.model.TotpAuthenticationMethod;
import io.strategiz.data.user.model.User;
import io.strategiz.data.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.Optional;

import static dev.samstevens.totp.util.Utils.getDataUriForImage;

/**
 * Service for managing Time-Based One-Time Password (TOTP) authentication
 */
@Service
public class TotpService {
    private static final Logger log = LoggerFactory.getLogger(TotpService.class);
    
    private static final int DEFAULT_TOTP_CODE_DIGITS = 6;
    private static final int DEFAULT_PERIOD_SECONDS = 30;
    private static final String TOTP_ALGORITHM = "SHA1"; // Most compatible with authenticator apps
    private static final String ISSUER = "Strategiz";
    
    private final SecretGenerator secretGenerator;
    private final QrGenerator qrGenerator;
    private final TimeProvider timeProvider;
    private final CodeVerifier codeVerifier;
    private final UserRepository userRepository;
    private final SessionAuthBusiness sessionAuthBusiness;
    
    @Autowired
    public TotpService(UserRepository userRepository, SessionAuthBusiness sessionAuthBusiness) {
        this.userRepository = userRepository;
        this.sessionAuthBusiness = sessionAuthBusiness;
        
        // Initialize TOTP components
        this.secretGenerator = new DefaultSecretGenerator();
        this.qrGenerator = new ZxingPngQrGenerator();
        this.timeProvider = new SystemTimeProvider();
        
        // Configure code verification with default settings (6 digits, 30-second window)
        CodeGenerator codeGenerator = new DefaultCodeGenerator(HashingAlgorithm.SHA1, DEFAULT_TOTP_CODE_DIGITS);
        DefaultCodeVerifier verifier = new DefaultCodeVerifier(codeGenerator, timeProvider);
        verifier.setTimePeriod(DEFAULT_PERIOD_SECONDS);
        verifier.setAllowedTimePeriodDiscrepancy(1); // Allow 1 time period discrepancy for clock drift
        this.codeVerifier = verifier;
    }
    
    /**
     * Generates a new TOTP secret key
     *
     * @return Base32 encoded TOTP secret
     */
    public String generateSecret() {
        return secretGenerator.generate();
    }
    
    /**
     * Generates a URI for the QR code that can be scanned by authenticator apps
     *
     * @param secret The TOTP secret key
     * @param userIdentifier The user's identifier (email or username)
     * @return URI for QR code generation
     */
    public String generateQrCodeUri(String secret, String userIdentifier) {
        QrData data = new QrData.Builder()
                .label(userIdentifier)
                .secret(secret)
                .issuer(ISSUER)
                .algorithm(HashingAlgorithm.SHA1)
                .digits(DEFAULT_TOTP_CODE_DIGITS)
                .period(DEFAULT_PERIOD_SECONDS)
                .build();
        
        return data.getUri();
    }
    
    /**
     * Generates a QR code image as a data URI
     *
     * @param secret The TOTP secret key
     * @param userIdentifier The user's identifier (email or username)
     * @return Base64 encoded data URI of QR code image
     * @throws QrGenerationException if QR code generation fails
     */
    public String generateQrCodeImageUri(String secret, String userIdentifier) throws QrGenerationException {
        QrData data = new QrData.Builder()
                .label(userIdentifier)
                .secret(secret)
                .issuer(ISSUER)
                .algorithm(HashingAlgorithm.SHA1)
                .digits(DEFAULT_TOTP_CODE_DIGITS)
                .period(DEFAULT_PERIOD_SECONDS)
                .build();
        
        byte[] imageData = qrGenerator.generate(data);
        return getDataUriForImage(imageData, qrGenerator.getImageMimeType());
    }
    
    /**
     * Verifies a TOTP code against a secret
     *
     * @param secret The TOTP secret key
     * @param code The code to verify
     * @return true if code is valid, false otherwise
     */
    public boolean verifyCode(String secret, String code) {
        return codeVerifier.isValidCode(secret, code);
    }
    
    /**
     * Enables TOTP for a user by storing the secret
     *
     * @param userId The user ID
     * @param secret The TOTP secret
     * @param authName Optional name for the authenticator (e.g., "Work Phone")
     * @return true if successful, false otherwise
     */
    public boolean enableTotpForUser(String userId, String secret, String authName) {
        try {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                log.warn("User not found for TOTP enablement: {}", userId);
                return false;
            }
            
            User user = userOpt.get();
            
            // Create and add the TOTP authentication method
            String totpName = authName != null && !authName.isEmpty() ? authName : "Authenticator App";
            TotpAuthenticationMethod totpMethod = new TotpAuthenticationMethod(totpName, secret, userId);
            user.addAuthenticationMethod(totpMethod);
            
            // Save the updated user
            userRepository.save(user);
            log.info("TOTP enabled for user: {}", userId);
            return true;
        } catch (Exception e) {
            log.error("Error enabling TOTP for user: {}", userId, e);
            return false;
        }
    }
    
    /**
     * Disables TOTP for a user
     *
     * @param userId The user ID
     * @return true if successful, false otherwise
     */
    public boolean disableTotpForUser(String userId) {
        try {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                log.warn("User not found for TOTP disablement: {}", userId);
                return false;
            }
            
            User user = userOpt.get();
            
            // Remove all TOTP authentication methods
            boolean removed = user.getAuthenticationMethods().removeIf(
                    method -> method instanceof TotpAuthenticationMethod
            );
            
            if (removed) {
                userRepository.save(user);
                log.info("TOTP disabled for user: {}", userId);
                return true;
            } else {
                log.info("No TOTP methods found to disable for user: {}", userId);
                return false;
            }
        } catch (Exception e) {
            log.error("Error disabling TOTP for user: {}", userId, e);
            return false;
        }
    }
    
    /**
     * Authenticates a user using their TOTP code
     *
     * @param userId The user ID
     * @param code The TOTP code
     * @param deviceId Optional device ID
     * @param ipAddress IP address of the authentication request
     * @return Optional TokenPair if authentication succeeds, empty otherwise
     */
    public Optional<SessionAuthBusiness.TokenPair> authenticateWithTotp(
            String userId, String code, String deviceId, String ipAddress) {
        try {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                log.warn("User not found for TOTP authentication: {}", userId);
                return Optional.empty();
            }
            
            User user = userOpt.get();
            
            // Find a TOTP authentication method
            Optional<TotpAuthenticationMethod> totpMethodOpt = user.getAuthenticationMethods().stream()
                    .filter(method -> method instanceof TotpAuthenticationMethod)
                    .map(method -> (TotpAuthenticationMethod) method)
                    .findFirst();
            
            if (totpMethodOpt.isEmpty()) {
                log.warn("No TOTP authentication method found for user: {}", userId);
                return Optional.empty();
            }
            
            TotpAuthenticationMethod totpMethod = totpMethodOpt.get();
            String secret = totpMethod.getSecret();
            
            // Verify the TOTP code
            if (verifyCode(secret, code)) {
                // Generate authentication tokens
                SessionAuthBusiness.TokenPair tokenPair = sessionAuthBusiness.createTokenPair(
                        userId, deviceId, ipAddress, "user");
                log.info("TOTP authentication successful for user: {}", userId);
                return Optional.of(tokenPair);
            } else {
                log.warn("Invalid TOTP code provided for user: {}", userId);
                return Optional.empty();
            }
        } catch (Exception e) {
            log.error("Error during TOTP authentication for user: {}", userId, e);
            return Optional.empty();
        }
    }
    
    /**
     * Checks if a user has TOTP enabled
     *
     * @param userId The user ID
     * @return true if TOTP is enabled, false otherwise
     */
    public boolean isTotpEnabledForUser(String userId) {
        try {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                return false;
            }
            
            User user = userOpt.get();
            
            // Check if user has any TOTP authentication methods
            return user.getAuthenticationMethods().stream()
                    .anyMatch(method -> method instanceof TotpAuthenticationMethod);
        } catch (Exception e) {
            log.error("Error checking TOTP status for user: {}", userId, e);
            return false;
        }
    }
}
