package io.strategiz.client.stripe;

import io.strategiz.framework.secrets.controller.SecretManager;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class that loads Stripe credentials from Vault and injects them into
 * StripeConfig. This ensures all Stripe secrets are loaded from Vault at startup.
 *
 * Vault path: secret/strategiz/stripe
 * Required fields:
 * - api-secret-key: Stripe secret key (sk_live_xxx or sk_test_xxx)
 * - api-publishable-key: Stripe publishable key (pk_live_xxx or pk_test_xxx)
 * - webhook-secret: Stripe webhook signing secret (whsec_xxx)
 * - price-trader: Stripe Price ID for Trader tier
 * - price-strategist: Stripe Price ID for Strategist tier
 */
@Configuration
public class StripeVaultConfig {

	private static final Logger log = LoggerFactory.getLogger(StripeVaultConfig.class);

	private final SecretManager secretManager;

	private final StripeConfig stripeConfig;

	@Autowired
	public StripeVaultConfig(@Qualifier("vaultSecretService") SecretManager secretManager, StripeConfig stripeConfig) {
		this.secretManager = secretManager;
		this.stripeConfig = stripeConfig;
	}

	@PostConstruct
	public void loadStripePropertiesFromVault() {
		try {
			log.info("Loading Stripe configuration from Vault...");

			// Load Stripe secret key
			String secretKey = secretManager.readSecret("stripe.api-secret-key", null);
			if (secretKey != null && !secretKey.isEmpty()) {
				stripeConfig.setSecretKey(secretKey);
				log.info("Loaded Stripe secret key from Vault");
			}
			else {
				log.warn("Stripe secret key not found in Vault - payment features will be disabled");
			}

			// Load Stripe publishable key
			String publishableKey = secretManager.readSecret("stripe.api-publishable-key", null);
			if (publishableKey != null && !publishableKey.isEmpty()) {
				stripeConfig.setPublishableKey(publishableKey);
				log.info("Loaded Stripe publishable key from Vault");
			}

			// Load webhook secret
			String webhookSecret = secretManager.readSecret("stripe.webhook-secret", null);
			if (webhookSecret != null && !webhookSecret.isEmpty()) {
				stripeConfig.setWebhookSecret(webhookSecret);
				log.info("Loaded Stripe webhook secret from Vault");
			}

			// Load price IDs
			String traderPriceId = secretManager.readSecret("stripe.price-trader", null);
			if (traderPriceId != null && !traderPriceId.isEmpty()) {
				stripeConfig.setTraderPriceId(traderPriceId);
				log.info("Loaded Stripe Trader price ID from Vault");
			}

			String strategistPriceId = secretManager.readSecret("stripe.price-strategist", null);
			if (strategistPriceId != null && !strategistPriceId.isEmpty()) {
				stripeConfig.setStrategistPriceId(strategistPriceId);
				log.info("Loaded Stripe Strategist price ID from Vault");
			}

			if (stripeConfig.isConfigured()) {
				log.info("Stripe configuration loaded successfully");
			}
			else {
				log.warn("Stripe is not fully configured - some payment features may be unavailable");
			}

		}
		catch (Exception e) {
			log.error("Failed to load Stripe configuration from Vault", e);
		}
	}

}
