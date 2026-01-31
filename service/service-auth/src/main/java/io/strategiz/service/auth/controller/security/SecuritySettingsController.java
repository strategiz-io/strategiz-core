package io.strategiz.service.auth.controller.security;

import io.strategiz.business.tokenauth.MfaEnforcementBusiness;
import io.strategiz.data.auth.entity.AuthenticationMethodEntity;
import io.strategiz.data.auth.entity.AuthenticationMethodType;
import io.strategiz.data.auth.entity.AuthenticationMethodMetadata;
import io.strategiz.data.auth.repository.AuthenticationMethodRepository;
import io.strategiz.service.auth.service.passkey.AuthenticatorRegistry;
import io.strategiz.service.base.controller.BaseController;

import com.google.cloud.Timestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for user security settings management
 *
 * Provides a unified view of all authentication methods configured for a user. This is
 * used by the profile/security settings page to display and manage: - Passkeys - TOTP
 * (Authenticator App) - SMS OTP - OAuth connections (Google, Facebook)
 *
 * Endpoints: - GET /auth/security - Get all security settings for a user
 */
@RestController
@RequestMapping("/v1/auth/security")
public class SecuritySettingsController extends BaseController {

	@Override
	protected String getModuleName() {
		return "service-auth";
	}

	private static final Logger log = LoggerFactory.getLogger(SecuritySettingsController.class);

	private final AuthenticationMethodRepository authMethodRepository;

	private final MfaEnforcementBusiness mfaEnforcementBusiness;

	public SecuritySettingsController(AuthenticationMethodRepository authMethodRepository,
			MfaEnforcementBusiness mfaEnforcementBusiness) {
		this.authMethodRepository = authMethodRepository;
		this.mfaEnforcementBusiness = mfaEnforcementBusiness;
	}

	/**
	 * Convert Firestore Timestamp to ISO 8601 string for frontend
	 */
	private String toIsoString(Timestamp timestamp) {
		if (timestamp == null) {
			return null;
		}
		return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos())
			.atOffset(ZoneOffset.UTC)
			.format(DateTimeFormatter.ISO_INSTANT);
	}

	/**
	 * Convert Instant to ISO 8601 string for frontend
	 */
	private String toIsoString(Instant instant) {
		if (instant == null) {
			return null;
		}
		return instant.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
	}

	/**
	 * Get all security settings for a user
	 *
	 * GET /auth/security?userId={userId}
	 *
	 * Returns a comprehensive view of all authentication methods: - passkeys: List of
	 * registered passkeys - totp: TOTP configuration status - smsOtp: SMS OTP
	 * configuration status - oauth: Connected OAuth providers - privacy: Privacy settings
	 * (placeholder for future)
	 */
	@GetMapping
	public ResponseEntity<Map<String, Object>> getSecuritySettings(@RequestParam String userId) {
		logRequest("getSecuritySettings", userId);
		log.info("SecuritySettingsController: Fetching security settings for userId: {}", userId);

		// Get all authentication methods for the user
		List<AuthenticationMethodEntity> allMethods = authMethodRepository.findByUserId(userId);
		log.info("SecuritySettingsController: Found {} authentication methods for userId: {}", allMethods.size(),
				userId);

		// Log each method found
		allMethods.forEach(m -> log.info("SecuritySettingsController: Found method - type: {}, id: {}, isActive: {}",
				m.getAuthenticationMethod(), m.getId(), m.getIsActive()));

		Map<String, Object> securitySettings = new HashMap<>();

		// Process Passkeys
		List<Map<String, Object>> passkeys = new ArrayList<>();
		allMethods.stream().filter(m -> m.getAuthenticationMethod() == AuthenticationMethodType.PASSKEY).forEach(m -> {
			Map<String, Object> passkey = new HashMap<>();
			passkey.put("id", m.getId());
			passkey.put("name", m.getName() != null ? m.getName() : "Passkey");
			passkey.put("credentialId",
					m.getMetadataAsString(AuthenticationMethodMetadata.PasskeyMetadata.CREDENTIAL_ID));
			passkey.put("createdAt", toIsoString(m.getCreatedDate()));
			passkey.put("lastUsedAt", toIsoString(m.getLastUsedAt()));
			passkey.put("isActive", m.getIsActive());

			// Get AAGUID and resolve authenticator info
			String aaguid = m.getMetadataAsString(AuthenticationMethodMetadata.PasskeyMetadata.AAGUID);
			AuthenticatorRegistry.AuthenticatorInfo authInfo = AuthenticatorRegistry.getAuthenticator(aaguid);
			passkey.put("aaguid", aaguid);
			passkey.put("authenticatorName", authInfo.name());
			passkey.put("authenticatorLogo", authInfo.logoId());
			passkey.put("provider", authInfo.provider());

			// Get backup/sync status
			Object backupEligible = m.getMetadata(AuthenticationMethodMetadata.PasskeyMetadata.BACKUP_ELIGIBLE);
			Object backupState = m.getMetadata(AuthenticationMethodMetadata.PasskeyMetadata.BACKUP_STATE);
			passkey.put("backupEligible", Boolean.TRUE.equals(backupEligible));
			passkey.put("synced", Boolean.TRUE.equals(backupState));

			// Legacy device name field
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
			totp.put("createdAt", toIsoString(totpMethod.getCreatedDate()));
			totp.put("lastUsedAt", toIsoString(totpMethod.getLastUsedAt()));
		}
		else {
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
			String phoneNumber = smsMethod
				.getMetadataAsString(AuthenticationMethodMetadata.SmsOtpMetadata.PHONE_NUMBER);
			if (phoneNumber != null && phoneNumber.length() > 4) {
				smsOtp.put("phoneNumber", "***" + phoneNumber.substring(phoneNumber.length() - 4));
			}
			smsOtp.put("lastUsedAt", toIsoString(smsMethod.getLastUsedAt()));
		}
		else {
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
			google.put("connectedAt", toIsoString(googleMethod.getCreatedDate()));
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
			facebook.put("connectedAt", toIsoString(facebookMethod.getCreatedDate()));
		}
		oauth.put("facebook", facebook);

		securitySettings.put("oauth", oauth);

		// MFA Enforcement settings
		MfaEnforcementBusiness.MfaEnforcementSettings mfaSettings = mfaEnforcementBusiness
			.getEnforcementSettings(userId);
		Map<String, Object> mfaEnforcement = new HashMap<>();
		mfaEnforcement.put("enforced", mfaSettings.enforced());
		mfaEnforcement.put("minimumAcrLevel", mfaSettings.minimumAcrLevel());
		mfaEnforcement.put("currentAcrLevel", mfaSettings.currentAcrLevel());
		mfaEnforcement.put("canEnable", mfaSettings.canEnable());
		mfaEnforcement.put("configuredMethods", mfaSettings.configuredMethods().stream().map(m -> m.type()).toList());
		mfaEnforcement.put("strengthLabel", mfaSettings.strengthLabel());
		mfaEnforcement.put("upgradeHint", mfaSettings.upgradeHint());

		// Include detailed method info for UI
		List<Map<String, Object>> methodDetails = mfaSettings.configuredMethods().stream().map(m -> {
			Map<String, Object> detail = new HashMap<>();
			detail.put("id", m.id());
			detail.put("type", m.type());
			detail.put("name", m.name());
			detail.put("strengthLabel", m.strengthLabel());
			return detail;
		}).toList();
		mfaEnforcement.put("methodDetails", methodDetails);

		securitySettings.put("mfaEnforcement", mfaEnforcement);

		// Summary stats
		Map<String, Object> summary = new HashMap<>();
		summary.put("totalPasskeys", passkeys.size());
		summary.put("hasTotpEnabled", totp.get("enabled"));
		summary.put("hasSmsOtpEnabled", smsOtp.get("enabled"));
		summary.put("hasGoogleConnected", google.get("connected"));
		summary.put("hasFacebookConnected", facebook.get("connected"));
		summary.put("mfaEnforced", mfaSettings.enforced());
		securitySettings.put("summary", summary);

		logRequestSuccess("getSecuritySettings", userId, securitySettings);
		return createCleanResponse(securitySettings);
	}

	/**
	 * Update MFA enforcement settings
	 *
	 * PUT /auth/security/mfa-enforcement?userId={userId}
	 *
	 * Request body: { "enforced": true }
	 * @param userId the user ID
	 * @param request the enforcement settings update
	 * @return updated MFA enforcement settings
	 */
	@PutMapping("/mfa-enforcement")
	public ResponseEntity<Map<String, Object>> updateMfaEnforcement(@RequestParam String userId,
			@RequestBody Map<String, Object> request) {
		logRequest("updateMfaEnforcement", userId);
		log.info("SecuritySettingsController: Updating MFA enforcement for userId: {}", userId);

		Boolean enforced = (Boolean) request.get("enforced");
		if (enforced == null) {
			Map<String, Object> error = new HashMap<>();
			error.put("success", false);
			error.put("error", "Missing required field: enforced");
			return ResponseEntity.badRequest().body(error);
		}

		MfaEnforcementBusiness.UpdateResult result = mfaEnforcementBusiness.updateMfaEnforcement(userId, enforced);

		Map<String, Object> response = new HashMap<>();
		response.put("success", result.success());

		if (result.success()) {
			MfaEnforcementBusiness.MfaEnforcementSettings settings = result.settings();
			Map<String, Object> mfaEnforcement = new HashMap<>();
			mfaEnforcement.put("enforced", settings.enforced());
			mfaEnforcement.put("minimumAcrLevel", settings.minimumAcrLevel());
			mfaEnforcement.put("currentAcrLevel", settings.currentAcrLevel());
			mfaEnforcement.put("canEnable", settings.canEnable());
			mfaEnforcement.put("configuredMethods", settings.configuredMethods().stream().map(m -> m.type()).toList());
			mfaEnforcement.put("strengthLabel", settings.strengthLabel());
			mfaEnforcement.put("upgradeHint", settings.upgradeHint());
			response.put("mfaEnforcement", mfaEnforcement);

			log.info("MFA enforcement updated for userId: {} to: {}", userId, enforced);
		}
		else {
			response.put("error", result.error());
			log.warn("Failed to update MFA enforcement for userId: {}: {}", userId, result.error());
		}

		logRequestSuccess("updateMfaEnforcement", userId, response);
		return createCleanResponse(response);
	}

	/**
	 * Check if step-up authentication is required
	 *
	 * GET /auth/security/step-up-check?userId={userId}&currentAcr={acr}
	 *
	 * Returns whether step-up auth is needed and available MFA methods
	 */
	@GetMapping("/step-up-check")
	public ResponseEntity<Map<String, Object>> checkStepUpRequired(@RequestParam String userId,
			@RequestParam(defaultValue = "1") String currentAcr) {
		logRequest("checkStepUpRequired", userId);
		log.info("SecuritySettingsController: Checking step-up requirement for userId: {} with ACR: {}", userId,
				currentAcr);

		MfaEnforcementBusiness.StepUpCheckResult result = mfaEnforcementBusiness.checkStepUpRequired(userId,
				currentAcr);

		Map<String, Object> response = new HashMap<>();
		response.put("required", result.required());
		response.put("minimumAcrLevel", result.minimumAcrLevel());

		if (result.required()) {
			List<Map<String, Object>> methods = result.availableMethods().stream().map(m -> {
				Map<String, Object> method = new HashMap<>();
				method.put("id", m.id());
				method.put("type", m.type());
				method.put("name", m.name());
				method.put("strengthLabel", m.strengthLabel());
				return method;
			}).toList();
			response.put("availableMethods", methods);
		}

		logRequestSuccess("checkStepUpRequired", userId, response);
		return createCleanResponse(response);
	}

}
