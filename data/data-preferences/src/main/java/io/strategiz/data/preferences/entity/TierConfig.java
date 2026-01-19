package io.strategiz.data.preferences.entity;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.PropertyName;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.strategiz.data.base.entity.BaseEntity;
import io.strategiz.data.base.annotation.Collection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configurable tier settings stored at config/tiers/{tierId}.
 *
 * <p>This entity allows admin configuration of tier limits, pricing, and features
 * without code deploys. Changes take effect immediately.</p>
 *
 * <p>Firestore path: config/tiers/{tierId}</p>
 *
 * <p>Tier IDs: explorer, strategist, quant</p>
 *
 * <p>Example structure:</p>
 * <pre>
 * {
 *   tierId: "strategist",
 *   displayName: "Strategist",
 *   monthlyPriceCents: 19900,
 *   monthlyStratAllotment: 55000,
 *   limits: {
 *     monthlyAiCap: 55000,
 *     alerts: 3,
 *     strategies: 3,
 *     deployments: 3,
 *     bots: 1
 *   },
 *   aiModelAccess: ["gemini-2.5-flash", "gpt-4o-mini", ...],
 *   features: {
 *     backtesting: true,
 *     advancedLabs: false,
 *     prioritySupport: false
 *   }
 * }
 * </pre>
 *
 * @see SubscriptionTier
 * @see PlatformSubscription
 */
@Collection("tierConfig")
public class TierConfig extends BaseEntity {

	// Tier IDs (must match SubscriptionTier enum values)
	public static final String TIER_EXPLORER = "explorer";
	public static final String TIER_STRATEGIST = "strategist";
	public static final String TIER_QUANT = "quant";

	// Limits constants for unlimited values
	public static final int UNLIMITED = -1;

	@DocumentId
	@PropertyName("tierId")
	@JsonProperty("tierId")
	private String tierId;

	@PropertyName("displayName")
	@JsonProperty("displayName")
	private String displayName;

	@PropertyName("description")
	@JsonProperty("description")
	private String description;

	/**
	 * Monthly subscription price in USD cents.
	 * 0 = free tier (Explorer)
	 */
	@PropertyName("monthlyPriceCents")
	@JsonProperty("monthlyPriceCents")
	private Integer monthlyPriceCents;

	/**
	 * Monthly STRAT allotment included with subscription.
	 * Added to user's wallet each billing cycle.
	 */
	@PropertyName("monthlyStratAllotment")
	@JsonProperty("monthlyStratAllotment")
	private Long monthlyStratAllotment;

	/**
	 * Tier-specific limits.
	 * Use -1 for unlimited.
	 */
	@PropertyName("limits")
	@JsonProperty("limits")
	private TierLimits limits;

	/**
	 * List of AI model IDs accessible to this tier.
	 */
	@PropertyName("aiModelAccess")
	@JsonProperty("aiModelAccess")
	private List<String> aiModelAccess;

	/**
	 * Feature flags for this tier.
	 */
	@PropertyName("features")
	@JsonProperty("features")
	private Map<String, Boolean> features;

	/**
	 * Tier level for ordering (0 = lowest, higher = better).
	 */
	@PropertyName("level")
	@JsonProperty("level")
	private Integer level;

	/**
	 * Whether this tier is currently enabled/visible.
	 */
	@PropertyName("enabled")
	@JsonProperty("enabled")
	private Boolean enabled;

	/**
	 * Stripe Price ID for this tier.
	 */
	@PropertyName("stripePriceId")
	@JsonProperty("stripePriceId")
	private String stripePriceId;

	public TierConfig() {
		super();
		this.enabled = true;
		this.limits = new TierLimits();
		this.aiModelAccess = new ArrayList<>();
		this.features = new HashMap<>();
	}

	/**
	 * Create default Explorer (free) tier config.
	 */
	public static TierConfig createExplorerDefault() {
		TierConfig config = new TierConfig();
		config.setTierId(TIER_EXPLORER);
		config.setDisplayName("Explorer");
		config.setDescription("Free tier for exploring the platform");
		config.setMonthlyPriceCents(0);
		config.setMonthlyStratAllotment(5000L);
		config.setLevel(0);

		TierLimits limits = new TierLimits();
		limits.setMonthlyAiCap(5000L);
		limits.setAlerts(0);
		limits.setStrategies(0);
		limits.setDeployments(0);
		limits.setBots(0);
		config.setLimits(limits);

		config.setAiModelAccess(List.of(
				"gemini-2.5-flash", "gemini-1.5-flash", "gpt-4o-mini",
				"claude-haiku-4-5", "llama-3.1-8b-instruct-maas", "mistral-nemo"
		));

		Map<String, Boolean> features = new HashMap<>();
		features.put("backtesting", false);
		features.put("advancedLabs", false);
		features.put("prioritySupport", false);
		config.setFeatures(features);

		return config;
	}

	/**
	 * Create default Strategist tier config.
	 */
	public static TierConfig createStrategistDefault() {
		TierConfig config = new TierConfig();
		config.setTierId(TIER_STRATEGIST);
		config.setDisplayName("Strategist");
		config.setDescription("For active traders building strategies");
		config.setMonthlyPriceCents(19900);
		config.setMonthlyStratAllotment(55000L);
		config.setLevel(1);

		TierLimits limits = new TierLimits();
		limits.setMonthlyAiCap(55000L);
		limits.setAlerts(3);
		limits.setStrategies(3);
		limits.setDeployments(3);
		limits.setBots(1);
		config.setLimits(limits);

		config.setAiModelAccess(List.of(
				"gemini-2.5-flash", "gemini-2.5-pro", "gemini-1.5-flash", "gemini-1.5-pro",
				"gpt-4o-mini", "gpt-4o", "claude-haiku-4-5", "claude-sonnet-4-5",
				"llama-3.1-8b-instruct-maas", "llama-3.1-70b-instruct-maas",
				"mistral-nemo", "mistral-small", "mistral-large-2", "command-r"
		));

		Map<String, Boolean> features = new HashMap<>();
		features.put("backtesting", true);
		features.put("advancedLabs", false);
		features.put("prioritySupport", false);
		config.setFeatures(features);

		return config;
	}

	/**
	 * Create default Quant tier config.
	 */
	public static TierConfig createQuantDefault() {
		TierConfig config = new TierConfig();
		config.setTierId(TIER_QUANT);
		config.setDisplayName("Quant");
		config.setDescription("For power users and serious traders");
		config.setMonthlyPriceCents(22900);
		config.setMonthlyStratAllotment(65000L);
		config.setLevel(2);

		TierLimits limits = new TierLimits();
		limits.setMonthlyAiCap(65000L);
		limits.setAlerts(UNLIMITED);
		limits.setStrategies(UNLIMITED);
		limits.setDeployments(UNLIMITED);
		limits.setBots(UNLIMITED);
		config.setLimits(limits);

		config.setAiModelAccess(List.of(
				"gemini-2.5-flash", "gemini-2.5-pro", "gemini-1.5-flash", "gemini-1.5-pro",
				"gpt-4o-mini", "gpt-4o", "claude-haiku-4-5", "claude-sonnet-4-5", "claude-opus-4-5",
				"o1", "o1-mini", "llama-3.1-8b-instruct-maas", "llama-3.1-70b-instruct-maas",
				"llama-3.1-405b-instruct-maas", "mistral-nemo", "mistral-small", "mistral-large-2",
				"command-r", "command-r-plus"
		));

		Map<String, Boolean> features = new HashMap<>();
		features.put("backtesting", true);
		features.put("advancedLabs", true);
		features.put("prioritySupport", true);
		config.setFeatures(features);

		return config;
	}

	// Helper methods

	/**
	 * Check if this tier is free.
	 */
	public boolean isFree() {
		return monthlyPriceCents == null || monthlyPriceCents == 0;
	}

	/**
	 * Check if a model is allowed for this tier.
	 */
	public boolean isModelAllowed(String modelId) {
		return aiModelAccess != null && aiModelAccess.contains(modelId);
	}

	/**
	 * Check if a feature is enabled for this tier.
	 */
	public boolean hasFeature(String featureKey) {
		return features != null && Boolean.TRUE.equals(features.get(featureKey));
	}

	/**
	 * Check if alerts are unlimited.
	 */
	public boolean hasUnlimitedAlerts() {
		return limits != null && limits.getAlerts() != null && limits.getAlerts() == UNLIMITED;
	}

	/**
	 * Check if strategies are unlimited.
	 */
	public boolean hasUnlimitedStrategies() {
		return limits != null && limits.getStrategies() != null && limits.getStrategies() == UNLIMITED;
	}

	// Getters and Setters

	@Override
	public String getId() {
		return tierId;
	}

	@Override
	public void setId(String id) {
		this.tierId = id;
	}

	public String getTierId() {
		return tierId;
	}

	public void setTierId(String tierId) {
		this.tierId = tierId;
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

	public Integer getMonthlyPriceCents() {
		return monthlyPriceCents;
	}

	public void setMonthlyPriceCents(Integer monthlyPriceCents) {
		this.monthlyPriceCents = monthlyPriceCents;
	}

	public Long getMonthlyStratAllotment() {
		return monthlyStratAllotment;
	}

	public void setMonthlyStratAllotment(Long monthlyStratAllotment) {
		this.monthlyStratAllotment = monthlyStratAllotment;
	}

	public TierLimits getLimits() {
		return limits;
	}

	public void setLimits(TierLimits limits) {
		this.limits = limits;
	}

	public List<String> getAiModelAccess() {
		return aiModelAccess;
	}

	public void setAiModelAccess(List<String> aiModelAccess) {
		this.aiModelAccess = aiModelAccess;
	}

	public Map<String, Boolean> getFeatures() {
		return features;
	}

	public void setFeatures(Map<String, Boolean> features) {
		this.features = features;
	}

	public Integer getLevel() {
		return level;
	}

	public void setLevel(Integer level) {
		this.level = level;
	}

	public Boolean getEnabled() {
		return enabled;
	}

	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}

	public String getStripePriceId() {
		return stripePriceId;
	}

	public void setStripePriceId(String stripePriceId) {
		this.stripePriceId = stripePriceId;
	}

	/**
	 * Nested class for tier-specific limits.
	 */
	public static class TierLimits {

		@PropertyName("monthlyAiCap")
		@JsonProperty("monthlyAiCap")
		private Long monthlyAiCap;

		@PropertyName("alerts")
		@JsonProperty("alerts")
		private Integer alerts;

		@PropertyName("strategies")
		@JsonProperty("strategies")
		private Integer strategies;

		@PropertyName("deployments")
		@JsonProperty("deployments")
		private Integer deployments;

		@PropertyName("bots")
		@JsonProperty("bots")
		private Integer bots;

		public TierLimits() {
		}

		public Long getMonthlyAiCap() {
			return monthlyAiCap;
		}

		public void setMonthlyAiCap(Long monthlyAiCap) {
			this.monthlyAiCap = monthlyAiCap;
		}

		public Integer getAlerts() {
			return alerts;
		}

		public void setAlerts(Integer alerts) {
			this.alerts = alerts;
		}

		public Integer getStrategies() {
			return strategies;
		}

		public void setStrategies(Integer strategies) {
			this.strategies = strategies;
		}

		public Integer getDeployments() {
			return deployments;
		}

		public void setDeployments(Integer deployments) {
			this.deployments = deployments;
		}

		public Integer getBots() {
			return bots;
		}

		public void setBots(Integer bots) {
			this.bots = bots;
		}

	}

}
