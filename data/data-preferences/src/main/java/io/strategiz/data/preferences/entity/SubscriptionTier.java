package io.strategiz.data.preferences.entity;

import java.util.Arrays;
import java.util.List;

/**
 * Subscription tiers for Strategiz platform.
 */
public enum SubscriptionTier {

	SCOUT("scout", "Scout", 0, List.of("gemini-2.0-flash", "gpt-4o-mini"), 25, // AI chat messages/day
			"Free tier - Explore the markets"),

	TRADER("trader", "Trader", 1900, // $19.00 in cents
			List.of("gemini-2.0-flash", "gpt-4o-mini", "claude-haiku-4-5", "gpt-4o"), 200,
			"For active traders"),

	STRATEGIST("strategist", "Strategist", 4900, // $49.00 in cents
			List.of("gemini-2.0-flash", "gpt-4o-mini", "claude-haiku-4-5", "gpt-4o", "claude-opus-4-5",
					"claude-sonnet-4", "o1", "o1-mini"),
			500, // Cost protection - not unlimited
			"For power users and serious traders");

	private final String id;

	private final String displayName;

	private final int priceInCents; // Monthly price in cents

	private final List<String> allowedModels;

	private final int dailyMessageLimit; // AI chat messages (Learn + Labs)

	private final String description;

	SubscriptionTier(String id, String displayName, int priceInCents, List<String> allowedModels, int dailyMessageLimit,
			String description) {
		this.id = id;
		this.displayName = displayName;
		this.priceInCents = priceInCents;
		this.allowedModels = allowedModels;
		this.dailyMessageLimit = dailyMessageLimit;
		this.description = description;
	}

	public String getId() {
		return id;
	}

	public String getDisplayName() {
		return displayName;
	}

	public int getPriceInCents() {
		return priceInCents;
	}

	public double getPriceInDollars() {
		return priceInCents / 100.0;
	}

	public List<String> getAllowedModels() {
		return allowedModels;
	}

	public int getDailyMessageLimit() {
		return dailyMessageLimit;
	}

	public String getDescription() {
		return description;
	}

	public boolean isModelAllowed(String modelId) {
		return allowedModels.contains(modelId);
	}

	public boolean hasUnlimitedMessages() {
		return false; // No tier has unlimited messages for cost protection
	}

	public static SubscriptionTier fromId(String id) {
		return Arrays.stream(values()).filter(tier -> tier.id.equalsIgnoreCase(id)).findFirst().orElse(SCOUT);
	}

	public static SubscriptionTier getDefault() {
		return SCOUT;
	}

}
