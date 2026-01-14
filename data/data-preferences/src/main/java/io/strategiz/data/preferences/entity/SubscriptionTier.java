package io.strategiz.data.preferences.entity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Subscription tiers for Strategiz platform with credit-based pricing.
 *
 * <p>Credit System: 1 credit = $0.001 in AI costs (1/10th of a cent)</p>
 *
 * <p>Tier Levels (ordinal positions):</p>
 * <ul>
 *   <li>Level 0: TRIAL (30-day free trial)</li>
 *   <li>Level 1: EXPLORER (entry paid tier)</li>
 *   <li>Level 2: STRATEGIST (mid tier)</li>
 *   <li>Level 3: QUANT (premium tier)</li>
 * </ul>
 *
 * <p>Use {@link #getLevel()} and {@link #meetsMinimumLevel(int)} to check tier access
 * instead of hardcoding tier names.</p>
 */
public enum SubscriptionTier {

	TRIAL("trial", "Trial", 0,
			List.of("gemini-2.5-flash", "gemini-1.5-flash", "gpt-4o-mini", "claude-haiku-4-5",
					"llama-3.1-8b-instruct-maas", "mistral-nemo"),
			40000, // 40K credits for trial
			"30-day free trial"),

	EXPLORER("explorer", "Explorer", 14900, // $149.00 in cents
			List.of("gemini-2.5-flash", "gemini-1.5-flash", "gpt-4o-mini", "claude-haiku-4-5",
					"llama-3.1-8b-instruct-maas", "mistral-nemo"),
			40000, // 40K credits
			"For new traders exploring the platform"),

	STRATEGIST("strategist", "Strategist", 19900, // $199.00 in cents
			List.of("gemini-2.5-flash", "gemini-2.5-pro", "gemini-1.5-flash", "gemini-1.5-pro",
					"gpt-4o-mini", "gpt-4o", "claude-haiku-4-5", "claude-sonnet-4-5",
					"llama-3.1-8b-instruct-maas", "llama-3.1-70b-instruct-maas",
					"mistral-nemo", "mistral-small", "mistral-large-2", "command-r"),
			55000, // 55K credits
			"For active traders building strategies"),

	QUANT("quant", "Quant", 22900, // $229.00 in cents
			List.of("gemini-2.5-flash", "gemini-2.5-pro", "gemini-1.5-flash", "gemini-1.5-pro",
					"gpt-4o-mini", "gpt-4o", "claude-haiku-4-5", "claude-sonnet-4-5", "claude-opus-4-5",
					"o1", "o1-mini", "llama-3.1-8b-instruct-maas", "llama-3.1-70b-instruct-maas",
					"llama-3.1-405b-instruct-maas", "mistral-nemo", "mistral-small", "mistral-large-2",
					"command-r", "command-r-plus"),
			65000, // 65K credits
			"For power users and serious traders");

	/**
	 * Model weights relative to Gemini Flash (1x baseline).
	 * Weight = blended cost per 1M tokens / $0.29 (Flash cost)
	 * Higher weight = more credits consumed per token.
	 */
	private static final Map<String, Integer> MODEL_WEIGHTS = Map.ofEntries(
			// Gemini models
			Map.entry("gemini-2.5-flash", 1),
			Map.entry("gemini-1.5-flash", 1),
			Map.entry("gemini-2.5-pro", 13),
			Map.entry("gemini-1.5-pro", 13),
			// OpenAI models
			Map.entry("gpt-4o-mini", 4),
			Map.entry("gpt-4o", 33),
			Map.entry("o1", 50),
			Map.entry("o1-mini", 15),
			// Claude models
			Map.entry("claude-haiku-4-5", 6),
			Map.entry("claude-sonnet-4-5", 23),
			Map.entry("claude-opus-4-5", 115),
			// Llama models
			Map.entry("llama-3.1-8b-instruct-maas", 1),
			Map.entry("llama-3.1-70b-instruct-maas", 3),
			Map.entry("llama-3.1-405b-instruct-maas", 10),
			// Mistral models
			Map.entry("mistral-nemo", 1),
			Map.entry("mistral-small", 2),
			Map.entry("mistral-large-2", 8),
			// Cohere models
			Map.entry("command-r", 2),
			Map.entry("command-r-plus", 10)
	);

	private final String id;

	private final String displayName;

	private final int priceInCents;

	private final List<String> allowedModels;

	private final int monthlyCredits;

	private final String description;

	SubscriptionTier(String id, String displayName, int priceInCents, List<String> allowedModels,
			int monthlyCredits, String description) {
		this.id = id;
		this.displayName = displayName;
		this.priceInCents = priceInCents;
		this.allowedModels = allowedModels;
		this.monthlyCredits = monthlyCredits;
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

	public int getMonthlyCredits() {
		return monthlyCredits;
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
	 */
	public boolean isTrial() {
		return this == TRIAL;
	}

	/**
	 * Get the tier level (ordinal position).
	 * TRIAL = 0, EXPLORER = 1, STRATEGIST = 2, QUANT = 3
	 *
	 * @return The tier level
	 */
	public int getLevel() {
		return ordinal();
	}

	/**
	 * Check if this tier meets or exceeds a minimum level.
	 *
	 * @param minimumLevel The minimum tier level required
	 * @return true if this tier level >= minimumLevel
	 */
	public boolean meetsMinimumLevel(int minimumLevel) {
		return ordinal() >= minimumLevel;
	}

	/**
	 * Get the weight multiplier for a model.
	 * Weight determines how many credits are consumed per token.
	 * Gemini Flash = 1x (baseline), Claude Opus = 115x.
	 *
	 * @param modelId The model ID
	 * @return The weight multiplier (defaults to 1 if unknown)
	 */
	public static int getModelWeight(String modelId) {
		return MODEL_WEIGHTS.getOrDefault(modelId, 1);
	}

	/**
	 * Calculate credits consumed for a given token usage.
	 *
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
		// Credits = tokens * weight * 0.3 / 1000 (where 0.3 is Flash credits per 1K tokens)
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
		return Arrays.stream(values())
				.filter(tier -> tier.id.equalsIgnoreCase(id))
				.findFirst()
				.orElse(TRIAL);
	}

	public static SubscriptionTier getDefault() {
		return TRIAL;
	}

	// Legacy compatibility methods (deprecated - will be removed in future version)

	/**
	 * @deprecated Use {@link #getMonthlyCredits()} instead. Daily message limits are replaced by monthly credits.
	 */
	@Deprecated(forRemoval = true)
	public int getDailyMessageLimit() {
		// Approximate conversion: monthly credits / 30 days
		return monthlyCredits / 30;
	}

	/**
	 * @deprecated Credits are not unlimited. Check remaining credits instead.
	 */
	@Deprecated(forRemoval = true)
	public boolean hasUnlimitedMessages() {
		return false;
	}

}
