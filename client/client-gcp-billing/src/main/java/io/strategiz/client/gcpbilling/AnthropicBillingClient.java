package io.strategiz.client.gcpbilling;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.strategiz.client.gcpbilling.model.AiProviderCost;
import io.strategiz.framework.secrets.service.VaultSecretService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Client for retrieving Anthropic Claude API costs and usage data. Uses Anthropic's
 * official Cost Report API.
 *
 * API Documentation: https://docs.anthropic.com/en/api/admin-api Required: Admin API key
 * (sk-ant-admin-...)
 *
 * Credentials loaded from Vault: secret/strategiz/ai/anthropic Required secrets:
 * admin-api-key
 *
 * Enable with: ai.billing.enabled=true
 */
@Component
@ConditionalOnProperty(name = "ai.billing.enabled", havingValue = "true", matchIfMissing = false)
public class AnthropicBillingClient {

	private static final Logger log = LoggerFactory.getLogger(AnthropicBillingClient.class);

	private static final String API_BASE_URL = "https://api.anthropic.com/v1";

	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

	private final RestTemplate restTemplate;

	private final ObjectMapper objectMapper;

	private final String adminApiKey;

	public AnthropicBillingClient(RestTemplate restTemplate, ObjectMapper objectMapper,
			VaultSecretService vaultSecretService) {
		this.restTemplate = restTemplate;
		this.objectMapper = objectMapper;

		// Load admin API key from Vault: secret/strategiz/ai/anthropic (admin-api-key
		// field)
		String vaultKey = null;
		try {
			vaultKey = vaultSecretService.readSecret("ai/anthropic.admin-api-key");
			if (vaultKey == null || vaultKey.isEmpty()) {
				log.warn("Admin API key not found in Vault at ai/anthropic.admin-api-key, "
						+ "falling back to environment variable");
				vaultKey = System.getenv("ANTHROPIC_ADMIN_API_KEY");
			}
		}
		catch (Exception e) {
			log.warn("Could not load Anthropic admin key from Vault: {}, falling back to environment variable",
					e.getMessage());
			vaultKey = System.getenv("ANTHROPIC_ADMIN_API_KEY");
		}
		this.adminApiKey = vaultKey;

		boolean configured = adminApiKey != null && !adminApiKey.isEmpty();
		log.info("AnthropicBillingClient initialized - Configured: {}", configured);
	}

	/**
	 * Get cost report for a date range using Anthropic's Cost Report API
	 * @param startDate Start date (inclusive)
	 * @param endDate End date (inclusive)
	 * @return AI provider cost data
	 */
	@Cacheable(value = "anthropicCosts", key = "#startDate.toString() + '-' + #endDate.toString()")
	public AiProviderCost getCostReport(LocalDate startDate, LocalDate endDate) {
		log.info("Fetching Anthropic cost report from {} to {}", startDate, endDate);

		if (adminApiKey == null || adminApiKey.isEmpty()) {
			log.warn("Anthropic Admin API key not configured");
			return AiProviderCost.empty("Anthropic", startDate, endDate);
		}

		try {
			String url = String.format("%s/organizations/cost_report?start_date=%s&end_date=%s", API_BASE_URL,
					startDate.format(DATE_FORMAT), endDate.format(DATE_FORMAT));

			HttpHeaders headers = new HttpHeaders();
			headers.set("x-api-key", adminApiKey);
			headers.set("anthropic-version", "2023-06-01");
			headers.setContentType(MediaType.APPLICATION_JSON);

			HttpEntity<String> entity = new HttpEntity<>(headers);
			ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

			if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
				return parseCostReport(response.getBody(), startDate, endDate);
			}

			log.warn("Unexpected response from Anthropic API: {}", response.getStatusCode());
			return AiProviderCost.empty("Anthropic", startDate, endDate);

		}
		catch (Exception e) {
			log.error("Error fetching Anthropic cost report: {}", e.getMessage(), e);
			return AiProviderCost.empty("Anthropic", startDate, endDate);
		}
	}

	/**
	 * Get usage report for token consumption details
	 */
	@Cacheable(value = "anthropicUsage", key = "#startDate.toString() + '-' + #endDate.toString()")
	public Map<String, Long> getUsageReport(LocalDate startDate, LocalDate endDate) {
		log.info("Fetching Anthropic usage report from {} to {}", startDate, endDate);

		if (adminApiKey == null || adminApiKey.isEmpty()) {
			return Map.of();
		}

		try {
			String url = String.format("%s/organizations/usage_report/messages?start_date=%s&end_date=%s", API_BASE_URL,
					startDate.format(DATE_FORMAT), endDate.format(DATE_FORMAT));

			HttpHeaders headers = new HttpHeaders();
			headers.set("x-api-key", adminApiKey);
			headers.set("anthropic-version", "2023-06-01");

			HttpEntity<String> entity = new HttpEntity<>(headers);
			ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

			if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
				return parseUsageReport(response.getBody());
			}

		}
		catch (Exception e) {
			log.error("Error fetching Anthropic usage report: {}", e.getMessage());
		}

		return Map.of();
	}

	private AiProviderCost parseCostReport(String jsonResponse, LocalDate startDate, LocalDate endDate) {
		try {
			JsonNode root = objectMapper.readTree(jsonResponse);

			BigDecimal totalCost = BigDecimal.ZERO;
			Map<String, BigDecimal> costByModel = new HashMap<>();
			Map<String, BigDecimal> costByService = new HashMap<>();
			long totalTokens = 0;
			long totalRequests = 0;

			// Parse cost breakdown
			if (root.has("costs")) {
				JsonNode costs = root.get("costs");
				for (JsonNode cost : costs) {
					String service = cost.has("service") ? cost.get("service").asText() : "unknown";
					double amount = cost.has("amount") ? cost.get("amount").asDouble() : 0.0;

					totalCost = totalCost.add(BigDecimal.valueOf(amount));
					costByService.put(service, BigDecimal.valueOf(amount));

					// Extract model info if available
					if (cost.has("model")) {
						String model = cost.get("model").asText();
						costByModel.merge(model, BigDecimal.valueOf(amount), BigDecimal::add);
					}
				}
			}

			// Parse usage stats if available
			if (root.has("total_tokens")) {
				totalTokens = root.get("total_tokens").asLong();
			}
			if (root.has("request_count")) {
				totalRequests = root.get("request_count").asLong();
			}

			log.info("Parsed Anthropic cost report: ${} total, {} tokens, {} requests", totalCost, totalTokens,
					totalRequests);

			return new AiProviderCost("Anthropic Claude", startDate, endDate, totalCost, costByModel, totalTokens,
					totalRequests, costByService);

		}
		catch (Exception e) {
			log.error("Error parsing Anthropic cost report: {}", e.getMessage());
			return AiProviderCost.empty("Anthropic", startDate, endDate);
		}
	}

	private Map<String, Long> parseUsageReport(String jsonResponse) {
		Map<String, Long> usage = new HashMap<>();
		try {
			JsonNode root = objectMapper.readTree(jsonResponse);
			if (root.has("total_input_tokens")) {
				usage.put("input_tokens", root.get("total_input_tokens").asLong());
			}
			if (root.has("total_output_tokens")) {
				usage.put("output_tokens", root.get("total_output_tokens").asLong());
			}
		}
		catch (Exception e) {
			log.error("Error parsing usage report: {}", e.getMessage());
		}
		return usage;
	}

}
