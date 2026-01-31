package io.strategiz.business.tokenauth;

import io.strategiz.data.auth.entity.AuthenticationMethodEntity;
import io.strategiz.data.auth.entity.AuthenticationMethodMetadata;
import io.strategiz.data.auth.entity.AuthenticationMethodType;
import io.strategiz.data.auth.repository.AuthenticationMethodRepository;
import io.strategiz.data.preferences.entity.SecurityPreferences;
import io.strategiz.data.preferences.repository.SecurityPreferencesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Business logic for MFA enforcement.
 *
 * <p>
 * This service manages MFA enforcement preferences and validates that users can enable
 * MFA based on their configured authentication methods.
 * </p>
 *
 * <p>
 * ACR Levels:
 * </p>
 * <ul>
 * <li>ACR 0: Partial/Signup (no enforcement possible)</li>
 * <li>ACR 1: Single-Factor (password only)</li>
 * <li>ACR 2: MFA (2+ methods OR passkey alone)</li>
 * <li>ACR 3: Strong MFA (passkey + another factor)</li>
 * </ul>
 */
@Component
public class MfaEnforcementBusiness {

	private static final Logger log = LoggerFactory.getLogger(MfaEnforcementBusiness.class);

	@Autowired
	private SecurityPreferencesRepository securityPreferencesRepository;

	@Autowired
	private AuthenticationMethodRepository authMethodRepository;

	/**
	 * Check if step-up authentication is required for a user based on their current ACR
	 * level.
	 * @param userId The user ID
	 * @param currentAcr The current session's ACR level (as string from token)
	 * @return StepUpCheckResult indicating if step-up is needed and available methods
	 */
	public StepUpCheckResult checkStepUpRequired(String userId, String currentAcr) {
		log.debug("Checking step-up requirement for user: {} with current ACR: {}", userId, currentAcr);

		SecurityPreferences prefs = securityPreferencesRepository.getByUserId(userId);

		if (!Boolean.TRUE.equals(prefs.getMfaEnforced())) {
			return StepUpCheckResult.notRequired();
		}

		int currentAcrLevel;
		try {
			currentAcrLevel = Integer.parseInt(currentAcr);
		}
		catch (NumberFormatException e) {
			currentAcrLevel = 1; // Default to single-factor
		}

		int minimumRequired = prefs.getMinimumAcrLevel() != null ? prefs.getMinimumAcrLevel()
				: SecurityPreferences.DEFAULT_MINIMUM_ACR_LEVEL;

		if (currentAcrLevel >= minimumRequired) {
			return StepUpCheckResult.notRequired();
		}

		// Step-up is required - get available MFA methods
		List<MfaMethodInfo> availableMethods = getAvailableMfaMethods(userId);

		log.info("Step-up required for user: {} (current ACR: {}, required: {})", userId, currentAcrLevel,
				minimumRequired);

		return StepUpCheckResult.required(minimumRequired, availableMethods);
	}

	/**
	 * Validate that a user can enable MFA enforcement. User must have at least one active
	 * MFA method configured.
	 * @param userId The user ID
	 * @return ValidationResult indicating if MFA can be enabled
	 */
	public ValidationResult validateCanEnableMfa(String userId) {
		log.debug("Validating if user {} can enable MFA enforcement", userId);

		List<AuthenticationMethodEntity> methods = authMethodRepository.findByUserId(userId);

		List<AuthenticationMethodEntity> activeMfaMethods = methods.stream()
			.filter(this::isMethodActive)
			.filter(m -> isMfaMethod(m.getAuthenticationMethod()))
			.collect(Collectors.toList());

		if (activeMfaMethods.isEmpty()) {
			log.warn("User {} cannot enable MFA enforcement: no MFA methods configured", userId);
			return ValidationResult.cannotEnable("No MFA methods configured. Set up at least one MFA method first.");
		}

		int achievableAcr = calculateMaxAchievableAcr(activeMfaMethods);
		List<String> configuredMethodTypes = activeMfaMethods.stream()
			.map(m -> m.getAuthenticationMethod().name().toLowerCase())
			.collect(Collectors.toList());

		log.info("User {} can enable MFA enforcement (achievable ACR: {}, methods: {})", userId, achievableAcr,
				configuredMethodTypes);

		return ValidationResult.canEnable(achievableAcr, configuredMethodTypes);
	}

	/**
	 * Get available MFA methods for a user.
	 * @param userId The user ID
	 * @return List of available MFA method info
	 */
	public List<MfaMethodInfo> getAvailableMfaMethods(String userId) {
		List<AuthenticationMethodEntity> methods = authMethodRepository.findByUserId(userId);

		return methods.stream()
			.filter(this::isMethodActive)
			.filter(m -> isMfaMethod(m.getAuthenticationMethod()))
			.map(this::toMfaMethodInfo)
			.collect(Collectors.toList());
	}

	/**
	 * Get the MFA enforcement settings for a user with computed fields populated.
	 * @param userId The user ID
	 * @return MfaEnforcementSettings with all settings and computed values
	 */
	public MfaEnforcementSettings getEnforcementSettings(String userId) {
		SecurityPreferences prefs = securityPreferencesRepository.getByUserId(userId);
		ValidationResult validation = validateCanEnableMfa(userId);

		List<MfaMethodInfo> configuredMethods = getAvailableMfaMethods(userId);

		String strengthLabel = calculateStrengthLabel(validation.maxAchievableAcr());
		String upgradeHint = calculateUpgradeHint(configuredMethods, validation.maxAchievableAcr());

		return new MfaEnforcementSettings(Boolean.TRUE.equals(prefs.getMfaEnforced()),
				prefs.getMinimumAcrLevel() != null ? prefs.getMinimumAcrLevel()
						: SecurityPreferences.DEFAULT_MINIMUM_ACR_LEVEL,
				validation.maxAchievableAcr(), validation.canEnable(), configuredMethods, strengthLabel, upgradeHint);
	}

	/**
	 * Enable or disable MFA enforcement for a user.
	 * @param userId The user ID
	 * @param enforced Whether to enforce MFA
	 * @return UpdateResult indicating success or failure
	 */
	public UpdateResult updateMfaEnforcement(String userId, boolean enforced) {
		log.info("Updating MFA enforcement for user {}: {}", userId, enforced);

		if (enforced) {
			ValidationResult validation = validateCanEnableMfa(userId);
			if (!validation.canEnable()) {
				log.warn("Cannot enable MFA enforcement for user {}: {}", userId, validation.reason());
				return UpdateResult.failed(validation.reason());
			}
		}

		SecurityPreferences prefs = securityPreferencesRepository.getByUserId(userId);
		prefs.setMfaEnforced(enforced);
		securityPreferencesRepository.save(userId, prefs);

		MfaEnforcementSettings updatedSettings = getEnforcementSettings(userId);

		log.info("MFA enforcement {} for user {}", enforced ? "enabled" : "disabled", userId);
		return UpdateResult.success(updatedSettings);
	}

	/**
	 * Called when an MFA method is about to be removed. Checks if this would disable MFA
	 * enforcement.
	 * @param userId The user ID
	 * @param methodType The method type being removed
	 * @return RemovalCheckResult indicating if removal would disable enforcement
	 */
	public RemovalCheckResult checkMfaMethodRemoval(String userId, AuthenticationMethodType methodType) {
		SecurityPreferences prefs = securityPreferencesRepository.getByUserId(userId);

		if (!Boolean.TRUE.equals(prefs.getMfaEnforced())) {
			return RemovalCheckResult.safeToRemove(false);
		}

		// Count remaining MFA methods after removal
		List<AuthenticationMethodEntity> methods = authMethodRepository.findByUserId(userId);
		long remainingMfaMethods = methods.stream()
			.filter(this::isMethodActive)
			.filter(m -> isMfaMethod(m.getAuthenticationMethod()))
			.filter(m -> m.getAuthenticationMethod() != methodType)
			.count();

		if (remainingMfaMethods == 0) {
			log.warn("Removing {} for user {} will disable MFA enforcement (last MFA method)", methodType, userId);
			return RemovalCheckResult.disablesEnforcement();
		}

		return RemovalCheckResult.safeToRemove(true);
	}

	/**
	 * Called after an MFA method is removed to update enforcement if needed.
	 * @param userId The user ID
	 */
	public void onMfaMethodRemoved(String userId) {
		ValidationResult validation = validateCanEnableMfa(userId);

		if (!validation.canEnable()) {
			// Auto-disable enforcement since no MFA methods remain
			securityPreferencesRepository.disableMfaEnforcement(userId);
			log.info("Auto-disabled MFA enforcement for user {} (no MFA methods remaining)", userId);
		}
	}

	// Helper methods

	/**
	 * Check if an authentication method is considered active. Passkeys are always
	 * considered active because they are verified during WebAuthn registration (the
	 * authenticator performs user verification). Other methods require explicit
	 * isActive=true flag.
	 */
	private boolean isMethodActive(AuthenticationMethodEntity method) {
		// Passkeys are verified during registration - treat as always active
		if (method.getAuthenticationMethod() == AuthenticationMethodType.PASSKEY) {
			return true;
		}
		// Other methods require explicit isActive flag
		return Boolean.TRUE.equals(method.getIsActive());
	}

	private boolean isMfaMethod(AuthenticationMethodType type) {
		return type == AuthenticationMethodType.TOTP || type == AuthenticationMethodType.PASSKEY
				|| type == AuthenticationMethodType.SMS_OTP || type == AuthenticationMethodType.DEVICE_TRUST;
	}

	private int calculateMaxAchievableAcr(List<AuthenticationMethodEntity> mfaMethods) {
		if (mfaMethods.isEmpty()) {
			return 1; // Only password
		}

		boolean hasPasskey = mfaMethods.stream()
			.anyMatch(m -> m.getAuthenticationMethod() == AuthenticationMethodType.PASSKEY);

		boolean hasOtherMfa = mfaMethods.stream()
			.anyMatch(m -> m.getAuthenticationMethod() != AuthenticationMethodType.PASSKEY);

		if (hasPasskey && hasOtherMfa) {
			return 3; // Strong MFA (passkey + another factor)
		}

		// Passkey alone or 2+ other factors = ACR 2
		return 2;
	}

	private String calculateStrengthLabel(int acrLevel) {
		return switch (acrLevel) {
			case 3 -> "Maximum";
			case 2 -> "Strong";
			case 1 -> "Basic";
			default -> "None";
		};
	}

	private String calculateUpgradeHint(List<MfaMethodInfo> methods, int currentAcr) {
		if (currentAcr >= 3) {
			return null; // Already at maximum
		}

		boolean hasPasskey = methods.stream().anyMatch(m -> "passkey".equalsIgnoreCase(m.type()));

		boolean hasTotp = methods.stream().anyMatch(m -> "totp".equalsIgnoreCase(m.type()));

		if (hasPasskey && !hasTotp) {
			return "Add an authenticator app alongside your passkey for maximum security (ACR 3).";
		}

		if (hasTotp && !hasPasskey) {
			return "Add a passkey alongside your authenticator app for maximum security (ACR 3).";
		}

		if (methods.isEmpty()) {
			return "Set up a passkey or authenticator app to enable MFA.";
		}

		return "Add a passkey for stronger authentication.";
	}

	private MfaMethodInfo toMfaMethodInfo(AuthenticationMethodEntity method) {
		String type = method.getAuthenticationMethod().name().toLowerCase();
		String name = method.getName();

		// Generate display name if not set
		if (name == null || name.isEmpty()) {
			name = switch (method.getAuthenticationMethod()) {
				case PASSKEY -> {
					String aaguid = method.getMetadataAsString(AuthenticationMethodMetadata.PasskeyMetadata.AAGUID);
					yield aaguid != null ? "Passkey" : "Passkey";
				}
				case TOTP -> "Authenticator App";
				case SMS_OTP -> {
					String phone = method.getMetadataAsString(AuthenticationMethodMetadata.SmsOtpMetadata.PHONE_NUMBER);
					yield phone != null ? "SMS (***" + phone.substring(Math.max(0, phone.length() - 4)) + ")"
							: "SMS OTP";
				}
				default -> method.getAuthenticationMethod().name();
			};
		}

		String strengthLabel = (method.getAuthenticationMethod() == AuthenticationMethodType.PASSKEY) ? "Strong"
				: "Standard";

		return new MfaMethodInfo(method.getId(), type, name, strengthLabel);
	}

	// Result records

	public record StepUpCheckResult(boolean required, int minimumAcrLevel, List<MfaMethodInfo> availableMethods) {
		public static StepUpCheckResult notRequired() {
			return new StepUpCheckResult(false, 0, List.of());
		}

		public static StepUpCheckResult required(int minimumAcr, List<MfaMethodInfo> methods) {
			return new StepUpCheckResult(true, minimumAcr, methods);
		}
	}

	public record ValidationResult(boolean canEnable, int maxAchievableAcr, List<String> configuredMethods,
			String reason) {
		public static ValidationResult canEnable(int maxAcr, List<String> methods) {
			return new ValidationResult(true, maxAcr, methods, null);
		}

		public static ValidationResult cannotEnable(String reason) {
			return new ValidationResult(false, 1, List.of(), reason);
		}
	}

	public record MfaEnforcementSettings(boolean enforced, int minimumAcrLevel, int currentAcrLevel, boolean canEnable,
			List<MfaMethodInfo> configuredMethods, String strengthLabel, String upgradeHint) {
	}

	public record MfaMethodInfo(String id, String type, String name, String strengthLabel) {
	}

	public record UpdateResult(boolean success, MfaEnforcementSettings settings, String error) {
		public static UpdateResult success(MfaEnforcementSettings settings) {
			return new UpdateResult(true, settings, null);
		}

		public static UpdateResult failed(String error) {
			return new UpdateResult(false, null, error);
		}
	}

	public record RemovalCheckResult(boolean safeToRemove, boolean willDisableMfaEnforcement,
			boolean enforcementWasActive) {
		public static RemovalCheckResult safeToRemove(boolean enforcementActive) {
			return new RemovalCheckResult(true, false, enforcementActive);
		}

		public static RemovalCheckResult disablesEnforcement() {
			return new RemovalCheckResult(true, true, true);
		}
	}

}
