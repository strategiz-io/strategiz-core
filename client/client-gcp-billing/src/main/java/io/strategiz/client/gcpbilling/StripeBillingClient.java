package io.strategiz.client.gcpbilling;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.strategiz.framework.secrets.service.VaultSecretService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

/**
 * Client for retrieving Stripe processing fees via the Balance Transactions API.
 *
 * API Documentation: https://docs.stripe.com/api/balance_transactions/list
 *
 * Credentials loaded from Vault: secret/strategiz/stripe Required secrets: api-secret-key
 *
 * Enable with: stripe.billing.enabled=true
 */
@Component
@ConditionalOnProperty(name = "stripe.billing.enabled", havingValue = "true", matchIfMissing = false)
public class StripeBillingClient {

	private static final Logger log = LoggerFactory.getLogger(StripeBillingClient.class);

	private static final String API_BASE_URL = "https://api.stripe.com/v1";

	private final RestTemplate restTemplate;

	private final ObjectMapper objectMapper;

	private final String apiSecretKey;

	public StripeBillingClient(RestTemplate restTemplate, ObjectMapper objectMapper,
			VaultSecretService vaultSecretService) {
		this.restTemplate = restTemplate;
		this.objectMapper = objectMapper;

		String vaultKey = null;
		try {
			vaultKey = vaultSecretService.readSecret("stripe.api-secret-key");
			if (vaultKey == null || vaultKey.isEmpty()) {
				log.warn("Stripe secret key not found in Vault at stripe.api-secret-key, "
						+ "falling back to environment variable");
				vaultKey = System.getenv("STRIPE_SECRET_KEY");
			}
		}
		catch (Exception e) {
			log.warn("Could not load Stripe secret key from Vault: {}, falling back to environment variable",
					e.getMessage());
			vaultKey = System.getenv("STRIPE_SECRET_KEY");
		}
		this.apiSecretKey = vaultKey;

		boolean configured = apiSecretKey != null && !apiSecretKey.isEmpty();
		log.info("StripeBillingClient initialized - Configured: {}", configured);
	}

	/**
	 * Get total Stripe fees for a date range by summing the fee field on balance
	 * transactions.
	 * @param startDate Start date (inclusive)
	 * @param endDate End date (inclusive)
	 * @return Map with "totalFees" (in dollars) and "transactionCount"
	 */
	@Cacheable(value = "stripeFees", key = "#startDate.toString() + '-' + #endDate.toString()")
	public Map<String, BigDecimal> getFeeSummary(LocalDate startDate, LocalDate endDate) {
		log.info("Fetching Stripe fee summary from {} to {}", startDate, endDate);

		Map<String, BigDecimal> result = new HashMap<>();
		result.put("totalFees", BigDecimal.ZERO);
		result.put("transactionCount", BigDecimal.ZERO);

		if (apiSecretKey == null || apiSecretKey.isEmpty()) {
			log.warn("Stripe API secret key not configured");
			return result;
		}

		try {
			long createdGte = startDate.atStartOfDay(ZoneOffset.UTC).toEpochSecond();
			long createdLte = endDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toEpochSecond() - 1;

			BigDecimal totalFeeCents = BigDecimal.ZERO;
			long transactionCount = 0;
			String startingAfter = null;
			boolean hasMore = true;

			while (hasMore) {
				String url = String.format("%s/balance_transactions?created[gte]=%d&created[lte]=%d&limit=100",
						API_BASE_URL, createdGte, createdLte);
				if (startingAfter != null) {
					url += "&starting_after=" + startingAfter;
				}

				HttpHeaders headers = new HttpHeaders();
				headers.setBasicAuth(apiSecretKey, "");

				HttpEntity<String> entity = new HttpEntity<>(headers);
				ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

				if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
					log.warn("Unexpected response from Stripe API: {}", response.getStatusCode());
					break;
				}

				JsonNode root = objectMapper.readTree(response.getBody());
				hasMore = root.has("has_more") && root.get("has_more").asBoolean();

				JsonNode data = root.get("data");
				if (data != null && data.isArray()) {
					for (JsonNode txn : data) {
						long fee = txn.has("fee") ? txn.get("fee").asLong() : 0;
						totalFeeCents = totalFeeCents.add(BigDecimal.valueOf(fee));
						transactionCount++;
						startingAfter = txn.get("id").asText();
					}
				}
				else {
					hasMore = false;
				}
			}

			BigDecimal totalFeeDollars = totalFeeCents.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
			result.put("totalFees", totalFeeDollars);
			result.put("transactionCount", BigDecimal.valueOf(transactionCount));

			log.info("Stripe fee summary: ${} total fees across {} transactions", totalFeeDollars, transactionCount);
		}
		catch (Exception e) {
			log.error("Error fetching Stripe fee summary: {}", e.getMessage(), e);
		}

		return result;
	}

	public boolean isConfigured() {
		return apiSecretKey != null && !apiSecretKey.isEmpty();
	}

}
