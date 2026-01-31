package io.strategiz.service.profile.constants;

import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.base.exception.ServiceBaseErrorDetails;

/**
 * Constants for the Profile Service module Contains all string literals, default values,
 * and configuration constants used throughout the profile service operations.
 */
public final class ProfileConstants {

	// Prevent instantiation
	private ProfileConstants() {
		throw new StrategizException(ServiceBaseErrorDetails.OPERATION_NOT_SUPPORTED, "ProfileConstants",
				"Constants class cannot be instantiated");
	}

	// Default Values
	public static final class Defaults {

		public static final String SUBSCRIPTION_TIER = io.strategiz.data.base.constants.SubscriptionTierConstants.DEFAULT;

		public static final boolean DEMO_MODE = true;

		public static final boolean EMAIL_VERIFIED = false;

		public static final boolean IS_ACTIVE = true;

		public static final boolean IS_PARTIAL_AUTH = true;

		private Defaults() {
		}

	}

	// Authentication Constants
	public static final class Auth {

		public static final String SIGNUP_DEVICE_ID = "signup-device";

		public static final String ACR_LEVEL_PARTIAL = "1";

		public static final String ACR_LEVEL_FULL = "2";

		private Auth() {
		}

	}

	// Subscription Tiers - delegates to SubscriptionTierConstants in data-framework-base
	public static final class SubscriptionTier {

		public static final String EXPLORER = io.strategiz.data.base.constants.SubscriptionTierConstants.EXPLORER;

		public static final String STRATEGIST = io.strategiz.data.base.constants.SubscriptionTierConstants.STRATEGIST;

		public static final String QUANT = io.strategiz.data.base.constants.SubscriptionTierConstants.QUANT;

		private SubscriptionTier() {
		}

	}

	// Trading Modes
	public static final class TradingMode {

		public static final String DEMO = "demo";

		public static final String LIVE = "live";

		private TradingMode() {
		}

	}

	// Error Messages
	public static final class ErrorMessages {

		public static final String USER_ALREADY_EXISTS = "User already exists with email: ";

		public static final String USER_NOT_FOUND = "User not found: ";

		public static final String PROFILE_NOT_FOUND = "Profile not found for user: ";

		public static final String PROFILE_NOT_FOUND_GENERIC = "Profile not found for user: ";

		private ErrorMessages() {
		}

	}

	// Log Messages
	public static final class LogMessages {

		public static final String CREATING_PROFILE = "Creating new user profile for email: ";

		public static final String PROFILE_CREATED = "Profile created successfully for user: ";

		public static final String PROFILE_UPDATED = "Profile updated successfully for user: ";

		public static final String SUBSCRIPTION_UPDATED = "Subscription tier updated successfully for user: {} to: {}";

		public static final String DEMO_MODE_UPDATED = "Demo mode updated successfully for user: {} to: {}";

		public static final String EMAIL_VERIFIED = "Email verified successfully for user: ";

		public static final String PROFILE_DELETED = "Profile deleted successfully for user: ";

		public static final String PROFILE_ALREADY_DELETED = "Profile already deleted for user: ";

		public static final String GENERATING_TOKEN = "Generating partial auth token for user: {} during signup";

		public static final String CREATING_SIGNUP_PROFILE = "Creating signup profile for email: ";

		public static final String USER_EXISTS_CONTINUE = "User already exists, returning existing profile for signup continuation: ";

		public static final String GENERATED_USER_ID = "Generated new user ID: {} for email: ";

		private LogMessages() {
		}

	}

}