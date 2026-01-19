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
	 */
	public static List<AiModelConfig> createDefaults() {
		List<AiModelConfig> models = new ArrayList<>();

		// Gemini models (Google)
		models.add(createModel("gemini-2.5-flash", "Gemini 2.5 Flash", PROVIDER_GOOGLE, 1,
				TierConfig.TIER_EXPLORER, "fast", "Fastest Google model"));
		models.add(createModel("gemini-1.5-flash", "Gemini 1.5 Flash", PROVIDER_GOOGLE, 1,
				TierConfig.TIER_EXPLORER, "fast", "Fast and efficient"));
		models.add(createModel("gemini-2.5-pro", "Gemini 2.5 Pro", PROVIDER_GOOGLE, 13,
				TierConfig.TIER_STRATEGIST, "powerful", "Most capable Google model"));
		models.add(createModel("gemini-1.5-pro", "Gemini 1.5 Pro", PROVIDER_GOOGLE, 13,
				TierConfig.TIER_STRATEGIST, "balanced", "Balanced performance"));

		// OpenAI models
		models.add(createModel("gpt-4o-mini", "GPT-4o Mini", PROVIDER_OPENAI, 4,
				TierConfig.TIER_EXPLORER, "fast", "Fast and affordable"));
		models.add(createModel("gpt-4o", "GPT-4o", PROVIDER_OPENAI, 33,
				TierConfig.TIER_STRATEGIST, "balanced", "Flagship model with vision"));
		models.add(createModel("o1", "o1", PROVIDER_OPENAI, 50,
				TierConfig.TIER_QUANT, "powerful", "Advanced reasoning"));
		models.add(createModel("o1-mini", "o1 Mini", PROVIDER_OPENAI, 15,
				TierConfig.TIER_QUANT, "balanced", "Efficient reasoning"));

		// Claude models (Anthropic)
		models.add(createModel("claude-haiku-4-5", "Claude Haiku 4.5", PROVIDER_ANTHROPIC, 6,
				TierConfig.TIER_EXPLORER, "fast", "Fastest Claude model"));
		models.add(createModel("claude-sonnet-4-5", "Claude Sonnet 4.5", PROVIDER_ANTHROPIC, 23,
				TierConfig.TIER_STRATEGIST, "balanced", "Balanced Claude model"));
		models.add(createModel("claude-opus-4-5", "Claude Opus 4.5", PROVIDER_ANTHROPIC, 115,
				TierConfig.TIER_QUANT, "powerful", "Most capable Claude model"));

		// Llama models (Meta)
		models.add(createModel("llama-3.1-8b-instruct-maas", "Llama 3.1 8B", PROVIDER_META, 1,
				TierConfig.TIER_EXPLORER, "fast", "Fast open source model"));
		models.add(createModel("llama-3.1-70b-instruct-maas", "Llama 3.1 70B", PROVIDER_META, 3,
				TierConfig.TIER_STRATEGIST, "balanced", "Powerful open source"));
		models.add(createModel("llama-3.1-405b-instruct-maas", "Llama 3.1 405B", PROVIDER_META, 10,
				TierConfig.TIER_QUANT, "powerful", "Largest Llama model"));

		// Mistral models
		models.add(createModel("mistral-nemo", "Mistral Nemo", PROVIDER_MISTRAL, 1,
				TierConfig.TIER_EXPLORER, "fast", "Fast Mistral model"));
		models.add(createModel("mistral-small", "Mistral Small", PROVIDER_MISTRAL, 2,
				TierConfig.TIER_STRATEGIST, "fast", "Efficient Mistral model"));
		models.add(createModel("mistral-large-2", "Mistral Large 2", PROVIDER_MISTRAL, 8,
				TierConfig.TIER_STRATEGIST, "balanced", "Capable Mistral model"));

		// Cohere models
		models.add(createModel("command-r", "Command R", PROVIDER_COHERE, 2,
				TierConfig.TIER_STRATEGIST, "fast", "Fast Cohere model"));
		models.add(createModel("command-r-plus", "Command R+", PROVIDER_COHERE, 10,
				TierConfig.TIER_QUANT, "balanced", "Most capable Cohere"));

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
