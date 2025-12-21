package io.strategiz.data.preferences.entity;

import java.util.Arrays;
import java.util.List;

/**
 * Subscription tiers for Strategiz platform.
 */
public enum SubscriptionTier {

	SCOUT("scout", "Scout", 0, List.of("gemini-3-flash-preview", "gemini-2.0-flash"), 20, 5,
			"Free tier - Explore the markets"),

	TRADER("trader", "Trader", 1900, // $19.00 in cents
			List.of("gemini-3-flash-preview", "gemini-3-pro-preview", "gemini-2.0-flash", "claude-haiku-4-5"), 200, 20,
			"For active traders"),

	STRATEGIST("strategist", "Strategist", 4900, // $49.00 in cents
			List.of("gemini-3-flash-preview", "gemini-3-pro-preview", "gemini-2.0-flash", "claude-opus-4-5",
					"claude-sonnet-4", "claude-haiku-4-5"),
			-1, // unlimited
			-1, // unlimited
			"For power users and serious traders");

	private final String id;

	private final String displayName;

	private final int priceInCents; // Monthly price in cents

	private final List<String> allowedModels;

	private final int dailyMessageLimit; // -1 for unlimited

	private final int dailyStrategyLimit; // -1 for unlimited

	private final String description;

	SubscriptionTier(String id, String displayName, int priceInCents, List<String> allowedModels, int dailyMessageLimit,
			int dailyStrategyLimit, String description) {
		this.id = id;
		this.displayName = displayName;
		this.priceInCents = priceInCents;
		this.allowedModels = allowedModels;
		this.dailyMessageLimit = dailyMessageLimit;
		this.dailyStrategyLimit = dailyStrategyLimit;
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

	public int getDailyStrategyLimit() {
		return dailyStrategyLimit;
	}

	public String getDescription() {
		return description;
	}

	public boolean isModelAllowed(String modelId) {
		return allowedModels.contains(modelId);
	}

	public boolean hasUnlimitedMessages() {
		return dailyMessageLimit == -1;
	}

	public boolean hasUnlimitedStrategies() {
		return dailyStrategyLimit == -1;
	}

	public static SubscriptionTier fromId(String id) {
		return Arrays.stream(values()).filter(tier -> tier.id.equalsIgnoreCase(id)).findFirst().orElse(SCOUT);
	}

	public static SubscriptionTier getDefault() {
		return SCOUT;
	}

}
