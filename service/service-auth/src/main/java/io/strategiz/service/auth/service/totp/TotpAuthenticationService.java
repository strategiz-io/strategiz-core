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
import io.strategiz.service.auth.service.totp.BaseTotpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Optional;

/**
 * Service for handling TOTP authentication operations
 * Manages TOTP code verification and authentication
 */
@Service
@DomainService(domain = "auth")
public class TotpAuthenticationService extends BaseTotpService {
    
    private static final Logger log = LoggerFactory.getLogger(TotpAuthenticationService.class);
    
    @Autowired
    private SessionAuthBusiness sessionAuthBusiness;
    
    public TotpAuthenticationService(UserRepository userRepository, 
                                     AuthenticationMethodRepository authMethodRepository) {
        super(userRepository, authMethodRepository);
    }
    
    /**
     * Verify a TOTP code for a user
     * @param userId the user ID
     * @param code the TOTP code
     * @return true if valid, false otherwise
     */
    @Override
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
        
        return isCodeValid(code, totpAuth.getMetadataAsString(AuthenticationMethodMetadata.TotpMetadata.SECRET_KEY));
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
                null                                                         // userAgent
            );
            SessionAuthBusiness.AuthResult authResult = sessionAuthBusiness.createAuthentication(authRequest);
            
            log.info("TOTP authentication successful for user: {}", userId);
            return new ApiTokenResponse(
                authResult.accessToken(),
                authResult.refreshToken(),
                userId
            );
        }
        
        throw new StrategizException(AuthErrors.USER_NOT_FOUND, userId);
    }
}
