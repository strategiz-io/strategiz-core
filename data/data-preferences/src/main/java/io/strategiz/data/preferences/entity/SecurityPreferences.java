package io.strategiz.data.preferences.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.PropertyName;
import io.strategiz.data.base.annotation.Collection;
import io.strategiz.data.base.entity.BaseEntity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Security preferences for users/{userId}/preferences/security
 * Contains user's MFA enforcement settings and security configurations.
 *
 * <p>MFA Enforcement Levels:</p>
 * <ul>
 *   <li>ACR 0: Partial/Signup (no enforcement possible)</li>
 *   <li>ACR 1: Single-Factor (password only)</li>
 *   <li>ACR 2: MFA (2+ methods OR passkey alone)</li>
 *   <li>ACR 3: Strong MFA (passkey + another factor)</li>
 * </ul>
 */
@Collection("preferences")
public class SecurityPreferences extends BaseEntity {

	public static final String PREFERENCE_ID = "security";

	public static final int DEFAULT_MINIMUM_ACR_LEVEL = 2;

	@DocumentId
	@PropertyName("preferenceId")
	@JsonProperty("preferenceId")
	private String preferenceId = PREFERENCE_ID;

	/**
	 * Single toggle: require ACR >= minimumAcrLevel for sign-in AND recovery
	 */
	@PropertyName("mfaEnforced")
	@JsonProperty("mfaEnforced")
	private Boolean mfaEnforced = false;

	/**
	 * Minimum ACR level required when mfaEnforced is true (default: 2 = standard MFA)
	 */
	@PropertyName("minimumAcrLevel")
	@JsonProperty("minimumAcrLevel")
	private Integer minimumAcrLevel = DEFAULT_MINIMUM_ACR_LEVEL;

	/**
	 * Timestamp when MFA enforcement was enabled
	 */
	@PropertyName("mfaEnforcedAt")
	@JsonProperty("mfaEnforcedAt")
	private Instant mfaEnforcedAt;

	/**
	 * Current maximum achievable ACR based on configured methods (computed, not stored)
	 */
	private transient Integer currentAcrLevel;

	/**
	 * List of configured MFA method types (computed, not stored)
	 */
	private transient List<String> configuredMethods;

	// Constructors
	public SecurityPreferences() {
		super();
		this.configuredMethods = new ArrayList<>();
	}

	// Getters and Setters
	public String getPreferenceId() {
		return preferenceId;
	}

	public void setPreferenceId(String preferenceId) {
		this.preferenceId = preferenceId;
	}

	public Boolean getMfaEnforced() {
		return mfaEnforced;
	}

	public void setMfaEnforced(Boolean mfaEnforced) {
		this.mfaEnforced = mfaEnforced;
		if (Boolean.TRUE.equals(mfaEnforced) && this.mfaEnforcedAt == null) {
			this.mfaEnforcedAt = Instant.now();
		}
		else if (Boolean.FALSE.equals(mfaEnforced)) {
			this.mfaEnforcedAt = null;
		}
	}

	public Integer getMinimumAcrLevel() {
		return minimumAcrLevel;
	}

	public void setMinimumAcrLevel(Integer minimumAcrLevel) {
		this.minimumAcrLevel = minimumAcrLevel != null ? minimumAcrLevel : DEFAULT_MINIMUM_ACR_LEVEL;
	}

	public Instant getMfaEnforcedAt() {
		return mfaEnforcedAt;
	}

	public void setMfaEnforcedAt(Instant mfaEnforcedAt) {
		this.mfaEnforcedAt = mfaEnforcedAt;
	}

	public Integer getCurrentAcrLevel() {
		return currentAcrLevel;
	}

	public void setCurrentAcrLevel(Integer currentAcrLevel) {
		this.currentAcrLevel = currentAcrLevel;
	}

	public List<String> getConfiguredMethods() {
		return configuredMethods;
	}

	public void setConfiguredMethods(List<String> configuredMethods) {
		this.configuredMethods = configuredMethods != null ? configuredMethods : new ArrayList<>();
	}

	/**
	 * Check if MFA enforcement is satisfied by the given ACR level
	 * @param acrLevel The current session's ACR level
	 * @return true if satisfied or enforcement not enabled
	 */
	public boolean isMfaSatisfied(int acrLevel) {
		if (!Boolean.TRUE.equals(mfaEnforced)) {
			return true; // No enforcement
		}
		return acrLevel >= (minimumAcrLevel != null ? minimumAcrLevel : DEFAULT_MINIMUM_ACR_LEVEL);
	}

	/**
	 * Check if step-up authentication is required
	 * @param currentAcr The current session's ACR level
	 * @return true if step-up is needed
	 */
	public boolean requiresStepUp(int currentAcr) {
		return Boolean.TRUE.equals(mfaEnforced) && !isMfaSatisfied(currentAcr);
	}

	// Required BaseEntity methods
	@Override
	public String getId() {
		return preferenceId;
	}

	@Override
	public void setId(String id) {
		this.preferenceId = id;
	}

}
