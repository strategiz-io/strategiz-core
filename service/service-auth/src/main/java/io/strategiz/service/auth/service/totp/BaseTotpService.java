package io.strategiz.service.auth.service.totp;

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
import io.strategiz.data.auth.entity.AuthenticationMethodEntity;
import io.strategiz.data.auth.entity.totp.TotpAuthenticationMethodEntity;
import io.strategiz.data.auth.repository.AuthenticationMethodRepository;
import io.strategiz.data.user.entity.UserEntity;
import io.strategiz.data.user.repository.UserRepository;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.auth.exception.AuthErrors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * Base class for TOTP services
 * Provides common functionality for TOTP operations
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
    protected final AuthenticationMethodRepository authMethodRepository;
    
    protected BaseTotpService(UserRepository userRepository, AuthenticationMethodRepository authMethodRepository) {
        this.userRepository = userRepository;
        this.authMethodRepository = authMethodRepository;
        this.secretGenerator = new DefaultSecretGenerator();
        this.qrGenerator = new ZxingPngQrGenerator();
        this.timeProvider = new SystemTimeProvider();
        
        // Configure code verifier with timing window
        this.codeVerifier = new DefaultCodeVerifier(
            new DefaultCodeGenerator(HashingAlgorithm.SHA1),
            timeProvider
        );
    }
    
    /**
     * Generate a QR code URI for TOTP setup
     * @param username the username
     * @param secret the TOTP secret
     * @return the QR code as a data URI string
     */
    protected String generateQrCodeUri(String username, String secret) {
        try {
            QrData data = new QrData.Builder()
                .label(username)
                .secret(secret)
                .issuer(ISSUER)
                .algorithm(HashingAlgorithm.SHA1)
                .digits(DEFAULT_TOTP_CODE_DIGITS)
                .period(DEFAULT_PERIOD_SECONDS)
                .build();
            
            byte[] qrCodeBytes = qrGenerator.generate(data);
            return "data:image/png;base64," + java.util.Base64.getEncoder().encodeToString(qrCodeBytes);
        } catch (QrGenerationException e) {
            log.error("Error generating QR code for user {}: {}", username, e.getMessage());
            throw new StrategizException(AuthErrors.QR_GENERATION_FAILED, e.getMessage());
        }
    }
    
    /**
     * Verify a TOTP code for a user
     * @param username the username
     * @param code the TOTP code
     * @return true if valid, false otherwise
     */
    protected boolean verifyCode(String username, String code) {
        Optional<UserEntity> userOpt = userRepository.findById(username);
        if (userOpt.isEmpty()) {
            log.warn("User not found for TOTP verification: {}", username);
            return false;
        }
        
        UserEntity user = userOpt.get();
        TotpAuthenticationMethodEntity totpAuth = findTotpAuthMethod(user);
        
        if (totpAuth == null || totpAuth.getSecret() == null) {
            log.warn("TOTP not configured for user: {}", username);
            return false;
        }
        
        return isCodeValid(code, totpAuth.getSecret());
    }
    
    /**
     * Validate a TOTP code against a secret
     * @param code the TOTP code
     * @param secret the secret
     * @return true if valid, false otherwise
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
    protected TotpAuthenticationMethodEntity findTotpAuthMethod(UserEntity user) {
        List<AuthenticationMethodEntity> authMethods = authMethodRepository.findByUserId(user.getId());
        if (authMethods != null) {
            for (AuthenticationMethodEntity method : authMethods) {
                if (method instanceof TotpAuthenticationMethodEntity && "TOTP".equals(method.getAuthenticationMethodType())) {
                    return (TotpAuthenticationMethodEntity) method;
                }
            }
        }
        return null;
    }
}
