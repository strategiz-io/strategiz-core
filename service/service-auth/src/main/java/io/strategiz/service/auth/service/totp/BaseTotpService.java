package io.strategiz.service.auth.service.totp;

import dev.samstevens.totp.code.*;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import io.strategiz.data.user.model.AuthenticationMethod;
import io.strategiz.data.user.model.TotpAuthenticationMethod;
import io.strategiz.data.user.model.User;
import io.strategiz.data.user.repository.UserRepository;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.auth.exception.AuthErrors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static dev.samstevens.totp.util.Utils.getDataUriForImage;

/**
 * Base service for TOTP functionality shared between registration and authentication
 */
public abstract class BaseTotpService {
    protected static final Logger log = LoggerFactory.getLogger(BaseTotpService.class);
    
    protected static final int DEFAULT_TOTP_CODE_DIGITS = 6;
    protected static final int DEFAULT_PERIOD_SECONDS = 30;
    protected static final String TOTP_ALGORITHM = "SHA1"; // Most compatible with authenticator apps
    protected static final String ISSUER = "Strategiz";
    
    protected final SecretGenerator secretGenerator;
    protected final QrGenerator qrGenerator;
    protected final TimeProvider timeProvider;
    protected final CodeVerifier codeVerifier;
    protected final UserRepository userRepository;
    
    protected BaseTotpService(UserRepository userRepository) {
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
     * Generate a QR code for the given TOTP secret
     * @param username the username
     * @param secret the TOTP secret
     * @return a data URI containing the QR code image
     */
    protected String generateQrCodeUri(String username, String secret) {
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
    protected boolean verifyCode(String username, String code) {
        Optional<User> userOpt = userRepository.findById(username);
        if (userOpt.isEmpty()) {
            log.warn("User not found: {}", username);
            return false;
        }
        User user = userOpt.get();
        
        // Find TOTP authentication method
        TotpAuthenticationMethod totpAuth = findTotpAuthMethod(user);
        
        if (totpAuth == null || totpAuth.getSecret() == null) {
            log.warn("TOTP authentication not set up for user: {}", username);
            return false;
        }
        
        // Verify the code
        boolean isValid = isCodeValid(code, totpAuth.getSecret());
        
        if (isValid) {
            log.info("TOTP code verified successfully for user: {}", username);
        } else {
            log.warn("Invalid TOTP code provided for user: {}", username);
        }
        
        return isValid;
    }
    
    /**
     * Check if a TOTP code is valid for a given secret
     * @param code the code to verify
     * @param secret the secret to verify against
     * @return true if the code is valid, false otherwise
     */
    protected boolean isCodeValid(String code, String secret) {
        // Use a slightly longer verification window to account for clock skew
        return codeVerifier.isValidCode(secret, code);
    }
    
    /**
     * Helper method to find the TOTP authentication method for a user
     * @param user the user
     * @return the TOTP authentication method, or null if not found
     */
    protected TotpAuthenticationMethod findTotpAuthMethod(User user) {
        if (user.getAuthenticationMethods() != null) {
            for (AuthenticationMethod method : user.getAuthenticationMethods()) {
                if (method instanceof TotpAuthenticationMethod && "TOTP".equals(method.getType())) {
                    return (TotpAuthenticationMethod) method;
                }
            }
        }
        return null;
    }
}
