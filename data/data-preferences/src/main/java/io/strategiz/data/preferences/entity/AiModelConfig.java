package io.strategiz.data.preferences.entity;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.PropertyName;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.strategiz.data.base.entity.BaseEntity;
import io.strategiz.data.base.annotation.Collection;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Configurable AI model settings stored at config/ai-models/{modelId}.
 *
 * <p>This entity allows admin configuration of AI model costs and tier requirements
 * without code deploys. Changes take effect immediately.</p>
 *
 * <p>Firestore path: config/ai-models/{modelId}</p>
 *
 * <p>Model Cost Calculation:</p>
 * <ul>
 *   <li>STRAT cost = (tokens / 1000) * stratCostPerKTokens * costMultiplier</li>
 *   <li>Cost is deducted from user's monthly AI cap (tier-controlled)</li>
 *   <li>STRAT balance is persistent; AI usage cap resets monthly</li>
 * </ul>
 *
 * <p>Example structure:</p>
 * <pre>
 * {
 *   modelId: "gpt-4o",
 *   displayName: "GPT-4o",
 *   provider: "openai",
 *   stratCostPerKTokens: 33,
 *   costMultiplier: 1.0,
 *   enabled: true,
 *   minTierRequired: "strategist"
 * }
 * </pre>
 *
 * @see TierConfig
 * @see SubscriptionTier
 */
@Collection("aiModelConfig")
public class AiModelConfig extends BaseEntity {

	// Provider constants
	public static final String PROVIDER_OPENAI = "openai";
	public static final String PROVIDER_ANTHROPIC = "anthropic";
	public static final String PROVIDER_GOOGLE = "google";
	public static final String PROVIDER_META = "meta";
	public static final String PROVIDER_MISTRAL = "mistral";
	public static final String PROVIDER_COHERE = "cohere";
	public static final String PROVIDER_XAI = "xai";

	@DocumentId
	@PropertyName("modelId")
	@JsonProperty("modelId")
	private String modelId;

	@PropertyName("displayName")
	@JsonProperty("displayName")
	private String displayName;

	@PropertyName("description")
	@JsonProperty("description")
	private String description;

	/**
	 * Provider name (openai, anthropic, google, meta, mistral, cohere).
	 */
	@PropertyName("provider")
	@JsonProperty("provider")
	private String provider;

	/**
	 * Base STRAT cost per 1000 tokens.
	 * This is the relative cost weight compared to the baseline model.
	 */
	@PropertyName("stratCostPerKTokens")
	@JsonProperty("stratCostPerKTokens")
	private Integer stratCostPerKTokens;

	/**
	 * Multiplier for cost adjustments (default 1.0).
	 * Can be used to quickly adjust pricing without changing base cost.
	 */
	@PropertyName("costMultiplier")
	@JsonProperty("costMultiplier")
	private BigDecimal costMultiplier;

	/**
	 * Whether this model is currently enabled.
	 */
	@PropertyName("enabled")
	@JsonProperty("enabled")
	private Boolean enabled;

	/**
	 * Minimum tier required to access this model.
	 * Values: explorer, strategist, quant
	 */
	@PropertyName("minTierRequired")
	@JsonProperty("minTierRequired")
	private String minTierRequired;

	/**
	 * Maximum tokens per request.
	 */
	@PropertyName("maxTokensPerRequest")
	@JsonProperty("maxTokensPerRequest")
	private Integer maxTokensPerRequest;

	/**
	 * Context window size.
	 */
	@PropertyName("contextWindow")
	@JsonProperty("contextWindow")
	private Integer contextWindow;

	/**
	 * Whether this model supports vision/image input.
	 */
	@PropertyName("supportsVision")
	@JsonProperty("supportsVision")
	private Boolean supportsVision;

	/**
	 * Whether this model supports function calling.
	 */
	@PropertyName("supportsFunctions")
	@JsonProperty("supportsFunctions")
	private Boolean supportsFunctions;

	/**
	 * Display order within provider group.
	 */
	@PropertyName("sortOrder")
	@JsonProperty("sortOrder")
	private Integer sortOrder;

	/**
	 * Category for grouping (e.g., "fast", "balanced", "powerful").
	 */
	@PropertyName("category")
	@JsonProperty("category")
	private String category;

	public AiModelConfig() {
		super();
		this.enabled = true;
		this.costMultiplier = BigDecimal.ONE;
		this.supportsVision = false;
		this.supportsFunctions = false;
		this.minTierRequired = TierConfig.TIER_EXPLORER;
	}

	/**
	 * Calculate effective STRAT cost for given token count.
	 */
	public int calculateStratCost(int promptTokens, int completionTokens) {
		int totalTokens = promptTokens + completionTokens;
		double baseCost = (totalTokens / 1000.0) * stratCostPerKTokens;
		double multiplier = costMultiplier != null ? costMultiplier.doubleValue() : 1.0;
		return (int) Math.ceil(baseCost * multiplier);
	}

	/**
	 * Check if this model requires at least the specified tier.
	 */
	public boolean requiresTier(String tierId) {
		if (minTierRequired == null) {
			return false;
		}
		return minTierRequired.equals(tierId);
	}

	/**
	 * Get tier level required (0=explorer, 1=strategist, 2=quant).
	 */
	public int getMinTierLevel() {
		if (minTierRequired == null) {
			return 0;
		}
		return switch (minTierRequired) {
			case TierConfig.TIER_STRATEGIST -> 1;
			case TierConfig.TIER_QUANT -> 2;
			default -> 0;
		};
	}

	/**
	 * Create all default AI model configs.
	 *
	 * Tier Organization:
	 * - EXPLORER (Free): Only free/very cheap models via Vertex AI (no direct API costs)
	 * - STRATEGIST (Paid): Budget-friendly models from all providers
	 * - QUANT (Premium): All models including flagship/reasoning models
	 */
	public static List<AiModelConfig> createDefaults() {
		List<AiModelConfig> models = new ArrayList<>();

		// ============================================================
		// EXPLORER TIER (Free) - Only free models via Vertex AI
		// No direct API costs - uses GCP credits/free tier
		// ============================================================
		models.add(createModel("gemini-2.5-flash", "Gemini 2.5 Flash", PROVIDER_GOOGLE, 1,
				TierConfig.TIER_EXPLORER, "fast", "Fastest Google model"));
		models.add(createModel("gemini-1.5-flash", "Gemini 1.5 Flash", PROVIDER_GOOGLE, 1,
				TierConfig.TIER_EXPLORER, "fast", "Fast and efficient"));
		models.add(createModel("llama-3.1-8b-instruct-maas", "Llama 3.1 8B", PROVIDER_META, 1,
				TierConfig.TIER_EXPLORER, "fast", "Fast open source model"));
		models.add(createModel("mistral-nemo", "Mistral Nemo", PROVIDER_MISTRAL, 1,
				TierConfig.TIER_EXPLORER, "fast", "Fast Mistral model"));
		models.add(createModel("grok-3-mini", "Grok 3 Mini", PROVIDER_XAI, 1,
				TierConfig.TIER_EXPLORER, "fast", "Free tier Grok model"));

		// ============================================================
		// STRATEGIST TIER (Paid) - Budget-friendly paid models
		// Includes: Haiku/Mini variants, older Sonnet, mid-tier models
		// ============================================================

		// Google
		models.add(createModel("gemini-2.5-pro", "Gemini 2.5 Pro", PROVIDER_GOOGLE, 15,
				TierConfig.TIER_STRATEGIST, "balanced", "Most capable Google model"));
		models.add(createModel("gemini-1.5-pro", "Gemini 1.5 Pro", PROVIDER_GOOGLE, 15,
				TierConfig.TIER_STRATEGIST, "balanced", "Balanced performance"));

		// OpenAI
		models.add(createModel("gpt-4o-mini", "GPT-4o Mini", PROVIDER_OPENAI, 4,
				TierConfig.TIER_STRATEGIST, "fast", "Fast and affordable"));
		models.add(createModel("gpt-4o", "GPT-4o", PROVIDER_OPENAI, 25,
				TierConfig.TIER_STRATEGIST, "balanced", "Flagship multimodal model"));

		// Anthropic
		models.add(createModel("claude-3-haiku", "Claude 3 Haiku", PROVIDER_ANTHROPIC, 3,
				TierConfig.TIER_STRATEGIST, "fast", "Budget-friendly, fast"));
		models.add(createModel("claude-3-sonnet", "Claude 3 Sonnet", PROVIDER_ANTHROPIC, 25,
				TierConfig.TIER_STRATEGIST, "balanced", "Budget-friendly, balanced"));
		models.add(createModel("claude-3-5-haiku", "Claude 3.5 Haiku", PROVIDER_ANTHROPIC, 8,
				TierConfig.TIER_STRATEGIST, "fast", "Fast & very affordable"));
		models.add(createModel("claude-3-5-sonnet", "Claude 3.5 Sonnet", PROVIDER_ANTHROPIC, 25,
				TierConfig.TIER_STRATEGIST, "balanced", "Excellent balanced model"));

		// Meta
		models.add(createModel("llama-3.1-70b-instruct-maas", "Llama 3.1 70B", PROVIDER_META, 3,
				TierConfig.TIER_STRATEGIST, "balanced", "Powerful open source"));

		// Mistral
		models.add(createModel("mistral-small", "Mistral Small", PROVIDER_MISTRAL, 2,
				TierConfig.TIER_STRATEGIST, "fast", "Efficient Mistral model"));
		models.add(createModel("mistral-large-2", "Mistral Large 2", PROVIDER_MISTRAL, 12,
				TierConfig.TIER_STRATEGIST, "balanced", "Capable Mistral model"));

		// Cohere
		models.add(createModel("command-r", "Command R", PROVIDER_COHERE, 2,
				TierConfig.TIER_STRATEGIST, "fast", "Fast Cohere model"));

		// xAI Grok
		models.add(createModel("grok-3", "Grok 3", PROVIDER_XAI, 25,
				TierConfig.TIER_STRATEGIST, "balanced", "General purpose model"));

		// ============================================================
		// QUANT TIER (Premium) - All flagship & reasoning models
		// Includes: Opus, o1, GPT-4 Turbo, Claude 4.5, Grok 4
		// ============================================================

		// OpenAI
		models.add(createModel("gpt-4-turbo", "GPT-4 Turbo", PROVIDER_OPENAI, 60,
				TierConfig.TIER_QUANT, "balanced", "Fast GPT-4 variant"));
		models.add(createModel("o1-mini", "o1 Mini", PROVIDER_OPENAI, 30,
				TierConfig.TIER_QUANT, "powerful", "Reasoning model, affordable"));
		models.add(createModel("o1", "o1", PROVIDER_OPENAI, 150,
				TierConfig.TIER_QUANT, "powerful", "Advanced reasoning model"));

		// Anthropic
		models.add(createModel("claude-3-opus", "Claude 3 Opus", PROVIDER_ANTHROPIC, 125,
				TierConfig.TIER_QUANT, "powerful", "Previous flagship model"));
		models.add(createModel("claude-haiku-4-5", "Claude Haiku 4.5", PROVIDER_ANTHROPIC, 8,
				TierConfig.TIER_QUANT, "fast", "Latest fast model"));
		models.add(createModel("claude-sonnet-4-5", "Claude Sonnet 4.5", PROVIDER_ANTHROPIC, 25,
				TierConfig.TIER_QUANT, "balanced", "Best balanced model"));
		models.add(createModel("claude-opus-4-5", "Claude Opus 4.5", PROVIDER_ANTHROPIC, 90,
				TierConfig.TIER_QUANT, "powerful", "Most capable for complex tasks"));

		// Meta
		models.add(createModel("llama-3.1-405b-instruct-maas", "Llama 3.1 405B", PROVIDER_META, 10,
				TierConfig.TIER_QUANT, "powerful", "Largest Llama model"));

		// Cohere
		models.add(createModel("command-r-plus", "Command R+", PROVIDER_COHERE, 18,
				TierConfig.TIER_QUANT, "balanced", "Most capable Cohere"));

		// xAI Grok
		models.add(createModel("grok-4", "Grok 4", PROVIDER_XAI, 30,
				TierConfig.TIER_QUANT, "powerful", "Advanced reasoning & coding"));
		models.add(createModel("grok-4.1-fast", "Grok 4.1 Fast", PROVIDER_XAI, 30,
				TierConfig.TIER_QUANT, "powerful", "Best for tool-calling, 2M context"));

		return models;
	}

	private static AiModelConfig createModel(String modelId, String displayName, String provider,
			int stratCostPerKTokens, String minTier, String category, String description) {
		AiModelConfig model = new AiModelConfig();
		model.setModelId(modelId);
		model.setDisplayName(displayName);
		model.setProvider(provider);
		model.setStratCostPerKTokens(stratCostPerKTokens);
		model.setMinTierRequired(minTier);
		model.setCategory(category);
		model.setDescription(description);
		return model;
	}

	// Getters and Setters

	@Override
	public String getId() {
		return modelId;
	}

	@Override
	public void setId(String id) {
		this.modelId = id;
	}

	public String getModelId() {
		return modelId;
	}

	public void setModelId(String modelId) {
		this.modelId = modelId;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getProvider() {
		return provider;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}

	public Integer getStratCostPerKTokens() {
		return stratCostPerKTokens;
	}

	public void setStratCostPerKTokens(Integer stratCostPerKTokens) {
		this.stratCostPerKTokens = stratCostPerKTokens;
	}

	public BigDecimal getCostMultiplier() {
		return costMultiplier;
	}

	public void setCostMultiplier(BigDecimal costMultiplier) {
		this.costMultiplier = costMultiplier;
	}

	public Boolean getEnabled() {
		return enabled;
	}

	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}

	public String getMinTierRequired() {
		return minTierRequired;
	}

	public void setMinTierRequired(String minTierRequired) {
		this.minTierRequired = minTierRequired;
	}

	public Integer getMaxTokensPerRequest() {
		return maxTokensPerRequest;
	}

	public void setMaxTokensPerRequest(Integer maxTokensPerRequest) {
		this.maxTokensPerRequest = maxTokensPerRequest;
	}

	public Integer getContextWindow() {
		return contextWindow;
	}

	public void setContextWindow(Integer contextWindow) {
		this.contextWindow = contextWindow;
	}

	public Boolean getSupportsVision() {
		return supportsVision;
	}

	public void setSupportsVision(Boolean supportsVision) {
		this.supportsVision = supportsVision;
	}

	public Boolean getSupportsFunctions() {
		return supportsFunctions;
	}

	public void setSupportsFunctions(Boolean supportsFunctions) {
		this.supportsFunctions = supportsFunctions;
	}

	public Integer getSortOrder() {
		return sortOrder;
	}

	public void setSortOrder(Integer sortOrder) {
		this.sortOrder = sortOrder;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

}
