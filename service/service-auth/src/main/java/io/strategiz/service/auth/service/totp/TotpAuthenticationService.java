package io.strategiz.service.auth.service.totp;

import java.time.Instant;
import io.strategiz.data.auth.entity.AuthenticationMethodEntity;
import io.strategiz.data.auth.entity.AuthenticationMethodType;
import io.strategiz.data.auth.entity.AuthenticationMethodMetadata;
import io.strategiz.data.auth.repository.AuthenticationMethodRepository;
import io.strategiz.data.user.entity.UserEntity;
import io.strategiz.data.user.repository.UserRepository;
import io.strategiz.service.auth.model.ApiTokenResponse;
import io.strategiz.business.tokenauth.SessionAuthBusiness;
import io.strategiz.framework.exception.DomainService;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.auth.exception.AuthErrors;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import io.strategiz.service.base.BaseService;

/**
 * Service for handling TOTP authentication operations
 * Manages TOTP code verification and authentication
 */
@Service
@DomainService(domain = "auth")
public class TotpAuthenticationService extends BaseService {

    @Override
    protected String getModuleName() {
        return "service-auth";
    }    
    private final UserRepository userRepository;
    private final AuthenticationMethodRepository authMethodRepository;
    private final CodeVerifier codeVerifier;
    
    @Autowired
    private SessionAuthBusiness sessionAuthBusiness;
    
    public TotpAuthenticationService(UserRepository userRepository, 
                                     AuthenticationMethodRepository authMethodRepository) {
        this.userRepository = userRepository;
        this.authMethodRepository = authMethodRepository;
        
        // Configure code verifier
        TimeProvider timeProvider = new SystemTimeProvider();
        this.codeVerifier = new DefaultCodeVerifier(
            new DefaultCodeGenerator(HashingAlgorithm.SHA1),
            timeProvider
        );
    }
    
    /**
     * Verify a TOTP code for a user
     * @param userId the user ID
     * @param code the TOTP code
     * @return true if valid, false otherwise
     */
    public boolean verifyCode(String userId, String code) {
        Optional<UserEntity> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            log.warn("User not found for TOTP verification: {}", userId);
            return false;
        }
        
        UserEntity user = userOpt.get();
        AuthenticationMethodEntity totpAuth = findTotpAuthMethod(user);
        
        if (totpAuth == null || totpAuth.getMetadataAsString(AuthenticationMethodMetadata.TotpMetadata.SECRET_KEY) == null) {
            log.warn("TOTP not configured for user: {}", userId);
            return false;
        }
        
        String secret = totpAuth.getMetadataAsString(AuthenticationMethodMetadata.TotpMetadata.SECRET_KEY);
        return codeVerifier.isValidCode(secret, code);
    }
    
    /**
     * Authenticate a user using TOTP and create session with tokens
     * @param userId the user ID
     * @param code the TOTP code
     * @param ipAddress client IP address
     * @param deviceId device identifier
     * @return API token response with access and refresh tokens
     * @throws StrategizException if authentication fails
     */
    public ApiTokenResponse authenticateWithTotp(String userId, String code, String ipAddress, String deviceId) {
        if (!verifyCode(userId, code)) {
            throw new StrategizException(AuthErrors.TOTP_VERIFICATION_FAILED, "Invalid TOTP code");
        }
        
        // Update last verified time
        Optional<UserEntity> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            UserEntity user = userOpt.get();
            AuthenticationMethodEntity totpAuth = findTotpAuthMethod(user);
            if (totpAuth != null) {
                totpAuth.setLastUsedAt(Instant.now());
                totpAuth.putMetadata(AuthenticationMethodMetadata.TotpMetadata.VERIFIED, true);
                totpAuth.putMetadata(AuthenticationMethodMetadata.TotpMetadata.LAST_USED_TIME, Instant.now().toString());
                totpAuth.putMetadata(AuthenticationMethodMetadata.TotpMetadata.VERIFICATION_TIME, Instant.now().toString());
                authMethodRepository.saveForUser(userId, totpAuth);
            }
            
            // Create session and tokens using SessionAuthBusiness
            SessionAuthBusiness.AuthRequest authRequest = new SessionAuthBusiness.AuthRequest(
                userId,                                                      // userId
                user.getProfile().getEmail(),                                // userEmail
                java.util.List.of("TOTP"),                                   // authenticationMethods  
                false,                                                       // isPartialAuth
                deviceId != null ? deviceId : "totp-auth-device",           // deviceId
                null,                                                        // deviceFingerprint
                ipAddress,                                                   // ipAddress
                null,                                                        // userAgent
                false // demoMode
            );
            SessionAuthBusiness.AuthResult authResult = sessionAuthBusiness.createAuthentication(authRequest);
            
            log.info("TOTP authentication successful for user: {}", userId);
            return new ApiTokenResponse(
                authResult.accessToken(),
                authResult.refreshToken(),
                "Bearer"
            );
        }
        
        throw new StrategizException(AuthErrors.USER_NOT_FOUND, userId);
    }
    
    /**
     * Helper method to find the TOTP authentication method for a user
     * @param user the user
     * @return the TOTP authentication method, or null if not found
     */
    private AuthenticationMethodEntity findTotpAuthMethod(UserEntity user) {
        List<AuthenticationMethodEntity> authMethods = authMethodRepository.findByUserIdAndType(
            user.getId(), AuthenticationMethodType.TOTP
        );
        
        // Return the first enabled TOTP method
        return authMethods.stream()
            .filter(method -> Boolean.TRUE.equals(method.getIsActive()))
            .findFirst()
            .orElse(null);
    }
}
