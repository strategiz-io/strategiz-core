package io.strategiz.service.auth.service.totp;

import java.time.LocalDateTime;
import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;

import io.strategiz.data.auth.entity.totp.TotpAuthenticationMethodEntity;
import io.strategiz.data.user.entity.UserEntity;
import io.strategiz.data.auth.entity.AuthenticationMethodEntity;
import io.strategiz.data.auth.repository.AuthenticationMethodRepository;
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
// @Service // Disabled - replaced by TotpRegistrationService and TotpAuthenticationService
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
    private final AuthenticationMethodRepository authMethodRepository;

    
    public TotpService(UserRepository userRepository, AuthenticationMethodRepository authMethodRepository) {
        this.userRepository = userRepository;
        this.authMethodRepository = authMethodRepository;
        
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
        Optional<UserEntity> userOpt = userRepository.findById(username);
        if (userOpt.isEmpty()) {
            throw new StrategizException(AuthErrors.USER_NOT_FOUND, username);
        }
        UserEntity user = userOpt.get();
        
        // Generate a new TOTP secret
        String secret = secretGenerator.generate();
        
        // Find existing TOTP method or create a new one  
        List<TotpAuthenticationMethodEntity> totpMethods = authMethodRepository.findByUserIdAndType(user.getId(), "TOTP")
            .stream()
            .map(TotpAuthenticationMethodEntity.class::cast)
            .collect(java.util.stream.Collectors.toList());
        TotpAuthenticationMethodEntity totpAuth = null;
        if (!totpMethods.isEmpty()) {
            totpAuth = totpMethods.get(0);
        } else {
            totpAuth = new TotpAuthenticationMethodEntity();
            totpAuth.setUserId(user.getId());
            totpAuth.setName("Authenticator App");
            authMethodRepository.save(totpAuth);
        }
        
        // Mark as not verified yet
        totpAuth.setSecret(secret);
        totpAuth.setLastAccessedAt(null); // Not verified yet
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
        Optional<UserEntity> userOpt = userRepository.findById(username);
        if (userOpt.isEmpty()) {
            log.warn("User not found: {}", username);
            return false;
        }
        UserEntity user = userOpt.get();
        
        // Find TOTP authentication method
        List<TotpAuthenticationMethodEntity> totpMethods = authMethodRepository.findByUserIdAndType(user.getId(), "TOTP")
            .stream()
            .map(TotpAuthenticationMethodEntity.class::cast)
            .collect(java.util.stream.Collectors.toList());
        TotpAuthenticationMethodEntity totpAuth = null;
        if (!totpMethods.isEmpty()) {
            totpAuth = totpMethods.get(0);
        }
        
        if (totpAuth == null || totpAuth.getSecret() == null) {
            log.warn("TOTP authentication not set up for user: {}", username);
            return false;
        }
        
        // Verify the code
        boolean isValid = codeVerifier.isValidCode(totpAuth.getSecret(), code);
        
        // If the code is valid, update the last verification time
        if (isValid) {
            totpAuth.setLastAccessedAt(java.time.Instant.now());
            authMethodRepository.save(totpAuth);
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
        Optional<UserEntity> userOpt = userRepository.findById(username);
        if (userOpt.isEmpty()) {
            return false;
        }
        UserEntity user = userOpt.get();
        
        // Find TOTP authentication method
        List<TotpAuthenticationMethodEntity> totpMethods = authMethodRepository.findByUserIdAndType(user.getId(), "TOTP")
            .stream()
            .map(TotpAuthenticationMethodEntity.class::cast)
            .collect(java.util.stream.Collectors.toList());
        if (!totpMethods.isEmpty()) {
            TotpAuthenticationMethodEntity totpAuth = totpMethods.get(0);
            // Consider verified if last verification time exists
            return totpAuth != null && totpAuth.getLastAccessedAt() != null;
        }
        return false;
    }
    
    /**
     * Disable TOTP for a user
     * @param username the username
     */
    public void disableTotp(String username) {
        Optional<UserEntity> userOpt = userRepository.findById(username);
        if (userOpt.isEmpty()) {
            log.warn("User not found: {}", username);
            return;
        }
        UserEntity user = userOpt.get();
        
        // Remove TOTP authentication
        List<TotpAuthenticationMethodEntity> totpMethods = authMethodRepository.findByUserIdAndType(user.getId(), "TOTP")
            .stream()
            .map(TotpAuthenticationMethodEntity.class::cast)
            .collect(java.util.stream.Collectors.toList());
        for (TotpAuthenticationMethodEntity method : totpMethods) {
            authMethodRepository.delete(method);
        }
        log.info("TOTP disabled for user {}", username);
    }
}
