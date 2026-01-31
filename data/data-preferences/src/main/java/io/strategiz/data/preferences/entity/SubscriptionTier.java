package io.strategiz.data.preferences.entity;

import io.strategiz.data.base.constants.SubscriptionTierConstants;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Subscription tiers for Strategiz platform with STRAT token-based pricing.
 *
 * <p>
 * STRAT System: Monthly STRAT allocation is credited to wallet on subscription. AI usage
 * debits STRAT from wallet, with a monthly cap per tier.
 * </p>
 *
 * <p>
 * Tier Levels (ordinal positions):
 * </p>
 * <ul>
 * <li>Level 0: EXPLORER (free freemium tier - 5,000 STRAT/mo)</li>
 * <li>Level 1: STRATEGIST (mid tier - $89/month - 25,000 STRAT/mo)</li>
 * <li>Level 2: QUANT (premium tier - $129/month - 40,000 STRAT/mo)</li>
 * </ul>
 *
 * <p>
 * Use {@link #getLevel()} and {@link #meetsMinimumLevel(int)} to check tier access
 * instead of hardcoding tier names.
 * </p>
 */
public enum SubscriptionTier {

	EXPLORER(SubscriptionTierConstants.EXPLORER, "Explorer", 0, // FREE freemium tier
			List.of("gemini-2.5-flash", "gemini-1.5-flash", "llama-3.1-8b-instruct-maas", "mistral-nemo",
					"grok-3-mini"),
			5000, // 5K STRAT
			"Free tier for exploring the platform"),

	STRATEGIST(SubscriptionTierConstants.STRATEGIST, "Strategist", 8900, // $89.00 in
																			// cents
			List.of("gemini-2.5-flash", "gemini-2.5-pro", "gemini-1.5-flash", "gemini-1.5-pro", "gpt-4o-mini", "gpt-4o",
					"claude-3-haiku", "claude-3-5-haiku", "claude-3-sonnet", "claude-3-5-sonnet",
					"llama-3.1-8b-instruct-maas", "llama-3.1-70b-instruct-maas", "mistral-nemo", "mistral-small",
					"mistral-large-2", "command-r", "grok-3-mini", "grok-3"),
			25000, // 25K STRAT
			"For active traders building strategies"),

	QUANT(SubscriptionTierConstants.QUANT, "Quant", 12900, // $129.00 in cents
			List.of("gemini-2.5-flash", "gemini-2.5-pro", "gemini-1.5-flash", "gemini-1.5-pro", "gpt-4o-mini", "gpt-4o",
					"gpt-4-turbo", "claude-3-haiku", "claude-3-5-haiku", "claude-3-sonnet", "claude-3-5-sonnet",
					"claude-haiku-4-5", "claude-sonnet-4-5", "claude-opus-4-5", "claude-3-opus", "o1", "o1-mini",
					"llama-3.1-8b-instruct-maas", "llama-3.1-70b-instruct-maas", "llama-3.1-405b-instruct-maas",
					"mistral-nemo", "mistral-small", "mistral-large-2", "command-r", "command-r-plus", "grok-3-mini",
					"grok-3", "grok-4", "grok-4.1-fast"),
			40000, // 40K STRAT
			"For power users and serious traders");

	/**
	 * Model weights relative to Gemini Flash (1x baseline). Weight = blended cost per 1M
	 * tokens / $0.29 (Flash cost) Higher weight = more STRAT consumed per token.
	 */
	private static final Map<String, Integer> MODEL_WEIGHTS = Map.ofEntries(
			// Gemini models
			Map.entry("gemini-2.5-flash", 1), Map.entry("gemini-1.5-flash", 1), Map.entry("gemini-2.5-pro", 15),
			Map.entry("gemini-1.5-pro", 15),
			// OpenAI models
			Map.entry("gpt-4o-mini", 4), Map.entry("gpt-4o", 25), Map.entry("gpt-4-turbo", 60), Map.entry("o1", 150),
			Map.entry("o1-mini", 30),
			// Claude models
			Map.entry("claude-3-haiku", 3), Map.entry("claude-3-5-haiku", 8), Map.entry("claude-haiku-4-5", 8),
			Map.entry("claude-3-sonnet", 25), Map.entry("claude-3-5-sonnet", 25), Map.entry("claude-sonnet-4-5", 25),
			Map.entry("claude-3-opus", 125), Map.entry("claude-opus-4-5", 90),
			// Llama models
			Map.entry("llama-3.1-8b-instruct-maas", 1), Map.entry("llama-3.1-70b-instruct-maas", 3),
			Map.entry("llama-3.1-405b-instruct-maas", 10),
			// Mistral models
			Map.entry("mistral-nemo", 1), Map.entry("mistral-small", 2), Map.entry("mistral-large-2", 12),
			// Cohere models
			Map.entry("command-r", 2), Map.entry("command-r-plus", 18),
			// xAI models
			Map.entry("grok-3-mini", 1), Map.entry("grok-3", 25), Map.entry("grok-4", 30),
			Map.entry("grok-4.1-fast", 30));

	private final String id;

	private final String displayName;

	private final int priceInCents;

	private final List<String> allowedModels;

	private final int monthlyStrat;

	private final String description;

	SubscriptionTier(String id, String displayName, int priceInCents, List<String> allowedModels, int monthlyStrat,
			String description) {
		this.id = id;
		this.displayName = displayName;
		this.priceInCents = priceInCents;
		this.allowedModels = allowedModels;
		this.monthlyStrat = monthlyStrat;
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

	public int getMonthlyStrat() {
		return monthlyStrat;
	}

	/**
	 * @deprecated Use {@link #getMonthlyStrat()} instead.
	 */
	@Deprecated(forRemoval = true)
	public int getMonthlyCredits() {
		return monthlyStrat;
	}

	public String getDescription() {
		return description;
	}

	public boolean isModelAllowed(String modelId) {
		return allowedModels.contains(modelId);
	}

	/**
	 * Check if this tier is a free tier (trial or free).
	 */
	public boolean isFree() {
		return priceInCents == 0;
	}

	/**
	 * Check if this is a trial tier.
	 * @deprecated Trial tier has been removed. Use {@link #isFree()} instead.
	 */
	@Deprecated(forRemoval = true)
	public boolean isTrial() {
		return false; // Trial tier removed - all users start on Explorer (free)
	}

	/**
	 * Get the tier level (ordinal position). EXPLORER = 0, STRATEGIST = 1, QUANT = 2
	 * @return The tier level
	 */
	public int getLevel() {
		return ordinal();
	}

	/**
	 * Check if this tier meets or exceeds a minimum level.
	 * @param minimumLevel The minimum tier level required
	 * @return true if this tier level >= minimumLevel
	 */
	public boolean meetsMinimumLevel(int minimumLevel) {
		return ordinal() >= minimumLevel;
	}

	/**
	 * Get the weight multiplier for a model. Weight determines how many STRAT are
	 * consumed per token. Gemini Flash = 1x (baseline), Claude 3 Opus = 125x.
	 * @param modelId The model ID
	 * @return The weight multiplier (defaults to 1 if unknown)
	 */
	public static int getModelWeight(String modelId) {
		return MODEL_WEIGHTS.getOrDefault(modelId, 1);
	}

	/**
	 * Calculate credits consumed for a given token usage.
	 * @param modelId The model used
	 * @param promptTokens Number of input tokens
	 * @param completionTokens Number of output tokens
	 * @return Credits consumed (rounded up)
	 */
	public static int calculateCredits(String modelId, int promptTokens, int completionTokens) {
		int weight = getModelWeight(modelId);
		int totalTokens = promptTokens + completionTokens;

		// 1 credit = $0.001 = 1000 tokens at baseline rate ($0.29/1M for Flash)
		// Credits = (tokens / 1000) * weight, but we use a more accurate formula:
		// Credits = tokens * weight * 0.3 / 1000 (where 0.3 is Flash credits per 1K
		// tokens)
		BigDecimal tokens = BigDecimal.valueOf(totalTokens);
		BigDecimal credits = tokens.multiply(BigDecimal.valueOf(weight))
			.multiply(BigDecimal.valueOf(0.3))
			.divide(BigDecimal.valueOf(1000), 0, RoundingMode.CEILING);

		return credits.intValue();
	}

	/**
	 * Get all model weights map.
	 * @return Unmodifiable map of model ID to weight
	 */
	public static Map<String, Integer> getAllModelWeights() {
		return MODEL_WEIGHTS;
	}

	public static SubscriptionTier fromId(String id) {
		// Handle legacy "trial" ID by returning EXPLORER
		if ("trial".equalsIgnoreCase(id)) {
			return EXPLORER;
		}
		return Arrays.stream(values()).filter(tier -> tier.id.equalsIgnoreCase(id)).findFirst().orElse(EXPLORER);
	}

	public static SubscriptionTier getDefault() {
		return EXPLORER;
	}

	// Legacy compatibility methods (deprecated - will be removed in future version)

	/**
	 * @deprecated Use {@link #getMonthlyStrat()} instead. Daily message limits are
	 * replaced by monthly STRAT.
	 */
	@Deprecated(forRemoval = true)
	public int getDailyMessageLimit() {
		// Approximate conversion: monthly STRAT / 30 days
		return monthlyStrat / 30;
	}

	/**
	 * @deprecated Credits are not unlimited. Check remaining credits instead.
	 */
	@Deprecated(forRemoval = true)
	public boolean hasUnlimitedMessages() {
		return false;
	}

}
