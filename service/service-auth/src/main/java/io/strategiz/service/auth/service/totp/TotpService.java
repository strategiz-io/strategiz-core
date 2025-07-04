package io.strategiz.service.auth.service.totp;

import dev.samstevens.totp.code.*;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;

import io.strategiz.data.user.model.TotpAuthenticationMethod;
import io.strategiz.data.user.model.User;
import io.strategiz.data.user.model.AuthenticationMethod;
import io.strategiz.data.user.repository.UserRepository;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.auth.exception.AuthErrors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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

    
    public TotpService(UserRepository userRepository) {
        this.userRepository = userRepository;
        
        // Initialize TOTP components
        this.secretGenerator = new DefaultSecretGenerator();
        this.qrGenerator = new ZxingPngQrGenerator();
        this.timeProvider = new SystemTimeProvider();
        
        // Configure the code verifier with our settings
        CodeGenerator codeGenerator = new DefaultCodeGenerator(
                HashingAlgorithm.valueOf(TOTP_ALGORITHM), DEFAULT_TOTP_CODE_DIGITS);
        this.codeVerifier = new DefaultCodeVerifier(codeGenerator, timeProvider);
    }
    
    /**
     * Generate a new TOTP secret for a user
     * @param username the username to generate the secret for
     * @return the generated QR code as a data URI
     */
    public String generateTotpSecret(String username) {
        Optional<User> userOpt = userRepository.findById(username);
        if (userOpt.isEmpty()) {
            throw new StrategizException(AuthErrors.USER_NOT_FOUND, username);
        }
        User user = userOpt.get();
        
        // Generate a new TOTP secret
        String secret = secretGenerator.generate();
        
        // Find existing TOTP method or create a new one
        TotpAuthenticationMethod totpAuth = null;
        if (user.getAuthenticationMethods() != null) {
            for (AuthenticationMethod method : user.getAuthenticationMethods()) {
                if (method instanceof TotpAuthenticationMethod && "TOTP".equals(method.getType())) {
                    totpAuth = (TotpAuthenticationMethod) method;
                    break;
                }
            }
        }
        
        if (totpAuth == null) {
            totpAuth = new TotpAuthenticationMethod();
            totpAuth.setType("TOTP");
            totpAuth.setName("Authenticator App");
            totpAuth.setCreatedBy(username);
            totpAuth.setModifiedBy(username);
            totpAuth.setCreatedAt(new Date());
            totpAuth.setModifiedAt(new Date());
            totpAuth.setIsActive(true);
            totpAuth.setVersion(1);
            
            if (user.getAuthenticationMethods() == null) {
                user.setAuthenticationMethods(new ArrayList<>());
            }
            user.addAuthenticationMethod(totpAuth);
        }
        
        // Mark as not verified yet
        totpAuth.setSecret(secret);
        totpAuth.setLastVerifiedAt(null); // Not verified yet
        userRepository.save(user);
        
        // Generate the QR code
        return generateQrCodeUri(username, secret);
    }
    
    /**
     * Generate a QR code for the given TOTP secret
     * @param username the username
     * @param secret the TOTP secret
     * @return a data URI containing the QR code image
     */
    private String generateQrCodeUri(String username, String secret) {
        try {
            // Create the QR code data
            QrData data = new QrData.Builder()
                    .label(username)
                    .secret(secret)
                    .issuer(ISSUER)
                    .algorithm(HashingAlgorithm.valueOf(TOTP_ALGORITHM))
                    .digits(DEFAULT_TOTP_CODE_DIGITS)
                    .period(DEFAULT_PERIOD_SECONDS)
                    .build();
            
            // Generate the QR code as a PNG image
            byte[] qrCodeImage = qrGenerator.generate(data);
            
            // Convert to a data URI that can be displayed in a browser
            return getDataUriForImage(qrCodeImage, qrGenerator.getImageMimeType());
        } catch (QrGenerationException e) {
            log.error("Error generating QR code", e);
            throw new StrategizException(AuthErrors.QR_GENERATION_FAILED, e.getMessage());
        }
    }
    
    /**
     * Verify a TOTP code for a user
     * @param username the username
     * @param code the TOTP code to verify
     * @return true if the code is valid, false otherwise
     */
    public boolean verifyCode(String username, String code) {
        Optional<User> userOpt = userRepository.findById(username);
        if (userOpt.isEmpty()) {
            log.warn("User not found: {}", username);
            return false;
        }
        User user = userOpt.get();
        
        // Find TOTP authentication method
        TotpAuthenticationMethod totpAuth = null;
        if (user.getAuthenticationMethods() != null) {
            for (AuthenticationMethod method : user.getAuthenticationMethods()) {
                if (method instanceof TotpAuthenticationMethod && "TOTP".equals(method.getType())) {
                    totpAuth = (TotpAuthenticationMethod) method;
                    break;
                }
            }
        }
        
        if (totpAuth == null || totpAuth.getSecret() == null) {
            log.warn("TOTP authentication not set up for user: {}", username);
            return false;
        }
        
        // Verify the code
        boolean isValid = codeVerifier.isValidCode(totpAuth.getSecret(), code);
        
        // If the code is valid, update the last verification time
        if (isValid) {
            totpAuth.setLastVerifiedAt(new Date());
            userRepository.save(user);
        }
        
        return isValid;
    }
    
    /**
     * Validates if the provided code matches the calculated TOTP code.
     * This method is prepared for future use in TOTP verification flows.
     * 
     * @param secret TOTP secret in Base32
     * @param code Code to verify
     * @return true if valid, false otherwise
     */
    @SuppressWarnings("unused")
    private boolean isCodeValid(String secret, String code) {
        // Use a slightly longer verification window to account for clock skew
        return codeVerifier.isValidCode(secret, code);
    }
    
    /**
     * Enable TOTP for a user's session
     * @param username the username
     * @param sessionToken the session token
     * @param code the TOTP code to verify
     * @return true if TOTP was successfully enabled, false otherwise
     */
    public boolean enableTotp(String username, String sessionToken, String code) {
        // Verify the code first
        if (!verifyCode(username, code)) {
            return false;
        }
        
        // Update the session to indicate TOTP is enabled
        // TODO: Implement method in SessionAuthBusiness
        log.info("TOTP verified for session {}", sessionToken);
        
        return true;
    }
    
    /**
     * Check if TOTP is set up for a user
     * @param username the username
     * @return true if TOTP is set up, false otherwise
     */
    public boolean isTotpSetUp(String username) {
        Optional<User> userOpt = userRepository.findById(username);
        if (userOpt.isEmpty()) {
            return false;
        }
        User user = userOpt.get();
        
        // Find TOTP authentication method
        if (user.getAuthenticationMethods() != null) {
            for (AuthenticationMethod method : user.getAuthenticationMethods()) {
                if (method instanceof TotpAuthenticationMethod && "TOTP".equals(method.getType())) {
                    TotpAuthenticationMethod totpAuth = (TotpAuthenticationMethod) method;
                    // Consider verified if last verification time exists
                    return totpAuth != null && totpAuth.getLastVerifiedAt() != null;
                }
            }
        }
        return false;
    }
    
    /**
     * Disable TOTP for a user
     * @param username the username
     */
    public void disableTotp(String username) {
        Optional<User> userOpt = userRepository.findById(username);
        if (userOpt.isEmpty()) {
            log.warn("User not found: {}", username);
            return;
        }
        User user = userOpt.get();
        
        // Remove TOTP authentication
        if (user.getAuthenticationMethods() != null) {
            List<AuthenticationMethod> updatedMethods = new ArrayList<>();
            for (AuthenticationMethod method : user.getAuthenticationMethods()) {
                if (!(method instanceof TotpAuthenticationMethod && "TOTP".equals(method.getType()))) {
                    updatedMethods.add(method);  // Keep all non-TOTP methods
                }
            }
            user.setAuthenticationMethods(updatedMethods);
            userRepository.save(user);
        }
    }
}
