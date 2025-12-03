package io.strategiz.service.auth.controller.security;

import io.strategiz.data.auth.entity.AuthenticationMethodEntity;
import io.strategiz.data.auth.entity.AuthenticationMethodType;
import io.strategiz.data.auth.entity.AuthenticationMethodMetadata;
import io.strategiz.data.auth.repository.AuthenticationMethodRepository;
import io.strategiz.service.base.controller.BaseController;
import io.strategiz.service.base.constants.ModuleConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for user security settings management
 *
 * Provides a unified view of all authentication methods configured for a user.
 * This is used by the profile/security settings page to display and manage:
 * - Passkeys
 * - TOTP (Authenticator App)
 * - SMS OTP
 * - OAuth connections (Google, Facebook)
 *
 * Endpoints:
 * - GET /auth/security - Get all security settings for a user
 */
@RestController
@RequestMapping("/v1/auth/security")
public class SecuritySettingsController extends BaseController {

    @Override
    protected String getModuleName() {
        return ModuleConstants.AUTH_MODULE;
    }

    private static final Logger log = LoggerFactory.getLogger(SecuritySettingsController.class);

    private final AuthenticationMethodRepository authMethodRepository;

    public SecuritySettingsController(AuthenticationMethodRepository authMethodRepository) {
        this.authMethodRepository = authMethodRepository;
    }

    /**
     * Get all security settings for a user
     *
     * GET /auth/security?userId={userId}
     *
     * Returns a comprehensive view of all authentication methods:
     * - passkeys: List of registered passkeys
     * - totp: TOTP configuration status
     * - smsOtp: SMS OTP configuration status
     * - oauth: Connected OAuth providers
     * - privacy: Privacy settings (placeholder for future)
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getSecuritySettings(@RequestParam String userId) {
        logRequest("getSecuritySettings", userId);
        log.info("SecuritySettingsController: Fetching security settings for userId: {}", userId);

        // Get all authentication methods for the user
        List<AuthenticationMethodEntity> allMethods = authMethodRepository.findByUserId(userId);
        log.info("SecuritySettingsController: Found {} authentication methods for userId: {}", allMethods.size(), userId);

        // Log each method found
        allMethods.forEach(m -> log.info("SecuritySettingsController: Found method - type: {}, id: {}, isActive: {}",
            m.getAuthenticationMethod(), m.getId(), m.getIsActive()));

        Map<String, Object> securitySettings = new HashMap<>();

        // Process Passkeys
        List<Map<String, Object>> passkeys = new ArrayList<>();
        allMethods.stream()
            .filter(m -> m.getAuthenticationMethod() == AuthenticationMethodType.PASSKEY)
            .forEach(m -> {
                Map<String, Object> passkey = new HashMap<>();
                passkey.put("id", m.getId());
                passkey.put("name", m.getDisplayInfo() != null ? m.getDisplayInfo() : "Passkey");
                passkey.put("createdAt", m.getCreatedDate());
                passkey.put("lastUsedAt", m.getLastUsedAt());
                passkey.put("isActive", m.getIsActive());
                // Get device info from metadata if available
                Object deviceName = m.getMetadata(AuthenticationMethodMetadata.PasskeyMetadata.DEVICE_NAME);
                if (deviceName != null) {
                    passkey.put("deviceName", deviceName);
                }
                passkeys.add(passkey);
            });
        securitySettings.put("passkeys", passkeys);

        // Process TOTP
        Map<String, Object> totp = new HashMap<>();
        AuthenticationMethodEntity totpMethod = allMethods.stream()
            .filter(m -> m.getAuthenticationMethod() == AuthenticationMethodType.TOTP)
            .findFirst()
            .orElse(null);

        if (totpMethod != null) {
            totp.put("enabled", totpMethod.getIsActive());
            totp.put("verified", totpMethod.getMetadata(AuthenticationMethodMetadata.TotpMetadata.VERIFIED));
            totp.put("createdAt", totpMethod.getCreatedDate());
            totp.put("lastUsedAt", totpMethod.getLastUsedAt());
        } else {
            totp.put("enabled", false);
            totp.put("verified", false);
        }
        securitySettings.put("totp", totp);

        // Process SMS OTP
        Map<String, Object> smsOtp = new HashMap<>();
        AuthenticationMethodEntity smsMethod = allMethods.stream()
            .filter(m -> m.getAuthenticationMethod() == AuthenticationMethodType.SMS_OTP)
            .filter(m -> Boolean.TRUE.equals(m.getMetadata(AuthenticationMethodMetadata.SmsOtpMetadata.IS_VERIFIED)))
            .findFirst()
            .orElse(null);

        if (smsMethod != null) {
            smsOtp.put("enabled", true);
            smsOtp.put("verified", true);
            // Mask phone number for security
            String phoneNumber = smsMethod.getMetadataAsString(AuthenticationMethodMetadata.SmsOtpMetadata.PHONE_NUMBER);
            if (phoneNumber != null && phoneNumber.length() > 4) {
                smsOtp.put("phoneNumber", "***" + phoneNumber.substring(phoneNumber.length() - 4));
            }
            smsOtp.put("lastUsedAt", smsMethod.getLastUsedAt());
        } else {
            smsOtp.put("enabled", false);
            smsOtp.put("verified", false);
        }
        securitySettings.put("smsOtp", smsOtp);

        // Process OAuth connections
        Map<String, Object> oauth = new HashMap<>();

        // Google
        AuthenticationMethodEntity googleMethod = allMethods.stream()
            .filter(m -> m.getAuthenticationMethod() == AuthenticationMethodType.OAUTH_GOOGLE)
            .findFirst()
            .orElse(null);
        Map<String, Object> google = new HashMap<>();
        google.put("connected", googleMethod != null && googleMethod.getIsActive());
        if (googleMethod != null) {
            google.put("email", googleMethod.getMetadataAsString("email"));
            google.put("connectedAt", googleMethod.getCreatedDate());
        }
        oauth.put("google", google);

        // Facebook
        AuthenticationMethodEntity facebookMethod = allMethods.stream()
            .filter(m -> m.getAuthenticationMethod() == AuthenticationMethodType.OAUTH_FACEBOOK)
            .findFirst()
            .orElse(null);
        Map<String, Object> facebook = new HashMap<>();
        facebook.put("connected", facebookMethod != null && facebookMethod.getIsActive());
        if (facebookMethod != null) {
            facebook.put("connectedAt", facebookMethod.getCreatedDate());
        }
        oauth.put("facebook", facebook);

        securitySettings.put("oauth", oauth);

        // Summary stats
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalPasskeys", passkeys.size());
        summary.put("hasTotpEnabled", totp.get("enabled"));
        summary.put("hasSmsOtpEnabled", smsOtp.get("enabled"));
        summary.put("hasGoogleConnected", google.get("connected"));
        summary.put("hasFacebookConnected", facebook.get("connected"));
        securitySettings.put("summary", summary);

        logRequestSuccess("getSecuritySettings", userId, securitySettings);
        return createCleanResponse(securitySettings);
    }
}
