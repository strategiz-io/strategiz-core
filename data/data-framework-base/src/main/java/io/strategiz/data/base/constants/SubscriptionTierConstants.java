package io.strategiz.data.base.constants;

/**
 * Subscription tier ID constants. This is the single source of truth for tier IDs across
 * all data modules.
 *
 * For full tier details (pricing, models, credits), see
 * {@code io.strategiz.data.preferences.entity.SubscriptionTier}
 */
public final class SubscriptionTierConstants {

	public static final String EXPLORER = "explorer";

	public static final String STRATEGIST = "strategist";

	public static final String QUANT = "quant";

	/**
	 * The default tier for new users.
	 */
	public static final String DEFAULT = EXPLORER;

	private SubscriptionTierConstants() {
		// Prevent instantiation
	}

}
