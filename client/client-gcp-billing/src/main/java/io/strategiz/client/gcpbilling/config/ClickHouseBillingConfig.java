package io.strategiz.client.gcpbilling.config;

import io.strategiz.framework.secrets.service.VaultSecretService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for ClickHouse Cloud Billing API.
 * Loads credentials from Vault for secure access to the ClickHouse Cloud API.
 *
 * Enable with: clickhouse.billing.enabled=true
 *
 * Vault secrets required at secret/strategiz/clickhouse-billing:
 * - organization-id: ClickHouse Cloud organization ID
 * - key-id: API key ID for HTTP Basic Auth
 * - key-secret: API key secret for HTTP Basic Auth
 */
@Configuration
@ConditionalOnProperty(name = "clickhouse.billing.enabled", havingValue = "true", matchIfMissing = false)
public class ClickHouseBillingConfig {

	private static final Logger log = LoggerFactory.getLogger(ClickHouseBillingConfig.class);

	@Value("${clickhouse.billing.api-url:https://api.clickhouse.cloud}")
	private String apiUrl;

	@Value("${clickhouse.billing.organization-id:}")
	private String organizationId;

	@Value("${clickhouse.billing.timeout-seconds:30}")
	private int timeoutSeconds;

	private final VaultSecretService vaultSecretService;

	public ClickHouseBillingConfig(VaultSecretService vaultSecretService) {
		this.vaultSecretService = vaultSecretService;
	}

	@Bean
	public ClickHouseBillingProperties clickHouseBillingProperties() {
		// Load from Vault first, with property fallbacks
		String vaultOrgId = organizationId;
		String vaultKeyId = null;
		String vaultKeySecret = null;

		try {
			if (vaultSecretService != null) {
				// Read from Vault path: secret/strategiz/clickhouse-billing
				String orgId = vaultSecretService.readSecret("clickhouse-billing.organization-id");
				if (orgId != null && !orgId.isEmpty()) {
					vaultOrgId = orgId;
				}

				vaultKeyId = vaultSecretService.readSecret("clickhouse-billing.key-id");
				vaultKeySecret = vaultSecretService.readSecret("clickhouse-billing.key-secret");
			}
		}
		catch (Exception e) {
			log.warn("Could not load ClickHouse billing config from Vault: {}", e.getMessage());
		}

		boolean configured = vaultKeyId != null && !vaultKeyId.isEmpty() && vaultKeySecret != null
				&& !vaultKeySecret.isEmpty() && vaultOrgId != null && !vaultOrgId.isEmpty();

		if (configured) {
			log.info("ClickHouse Billing Config - API URL: {}, Organization: {}, Key ID: {}..., Configured: true",
					apiUrl, vaultOrgId, vaultKeyId.substring(0, Math.min(8, vaultKeyId.length())));
		}
		else {
			log.warn(
					"ClickHouse Billing NOT CONFIGURED - Missing credentials in Vault at secret/strategiz/clickhouse-billing");
		}

		return new ClickHouseBillingProperties(apiUrl, vaultOrgId, vaultKeyId, vaultKeySecret, timeoutSeconds,
				configured);
	}

	/**
	 * Properties holder for ClickHouse Cloud billing configuration
	 */
	public record ClickHouseBillingProperties(String apiUrl, String organizationId, String keyId, String keySecret,
			int timeoutSeconds, boolean configured) {

		/**
		 * Get the usage cost endpoint URL
		 */
		public String getUsageCostUrl() {
			return String.format("%s/v1/organizations/%s/usageCost", apiUrl, organizationId);
		}

	}

}
