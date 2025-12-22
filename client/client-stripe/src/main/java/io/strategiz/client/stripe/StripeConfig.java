package io.strategiz.client.stripe;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Stripe API configuration. Initializes the Stripe SDK with API keys from Vault.
 */
@Configuration
public class StripeConfig {

	private static final Logger logger = LoggerFactory.getLogger(StripeConfig.class);

	@Value("${stripe.api.secret-key:}")
	private String secretKey;

	@Value("${stripe.api.publishable-key:}")
	private String publishableKey;

	@Value("${stripe.webhook.secret:}")
	private String webhookSecret;

	@Value("${stripe.price.trader:}")
	private String traderPriceId;

	@Value("${stripe.price.strategist:}")
	private String strategistPriceId;

	@Value("${app.base-url:https://strategiz.io}")
	private String appBaseUrl;

	@PostConstruct
	public void init() {
		if (secretKey != null && !secretKey.isEmpty()) {
			Stripe.apiKey = secretKey;
			logger.info("Stripe API initialized successfully");
		}
		else {
			logger.warn("Stripe API key not configured - payment features will be disabled");
		}
	}

	public String getSecretKey() {
		return secretKey;
	}

	public String getPublishableKey() {
		return publishableKey;
	}

	public String getWebhookSecret() {
		return webhookSecret;
	}

	public String getTraderPriceId() {
		return traderPriceId;
	}

	public String getStrategistPriceId() {
		return strategistPriceId;
	}

	public String getAppBaseUrl() {
		return appBaseUrl;
	}

	/**
	 * Get the Stripe Price ID for a given tier.
	 * @param tierId The tier ID (trader, strategist)
	 * @return The Stripe Price ID, or null if not found
	 */
	public String getPriceIdForTier(String tierId) {
		return switch (tierId.toLowerCase()) {
			case "trader" -> traderPriceId;
			case "strategist" -> strategistPriceId;
			default -> null;
		};
	}

	public boolean isConfigured() {
		return secretKey != null && !secretKey.isEmpty();
	}

	// Setters for Vault configuration injection
	public void setSecretKey(String secretKey) {
		this.secretKey = secretKey;
		// Re-initialize Stripe SDK with new key
		if (secretKey != null && !secretKey.isEmpty()) {
			Stripe.apiKey = secretKey;
		}
	}

	public void setPublishableKey(String publishableKey) {
		this.publishableKey = publishableKey;
	}

	public void setWebhookSecret(String webhookSecret) {
		this.webhookSecret = webhookSecret;
	}

	public void setTraderPriceId(String traderPriceId) {
		this.traderPriceId = traderPriceId;
	}

	public void setStrategistPriceId(String strategistPriceId) {
		this.strategistPriceId = strategistPriceId;
	}

}
