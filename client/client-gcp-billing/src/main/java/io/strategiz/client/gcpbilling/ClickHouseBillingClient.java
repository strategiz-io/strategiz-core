package io.strategiz.client.gcpbilling;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.strategiz.client.gcpbilling.config.ClickHouseBillingConfig.ClickHouseBillingProperties;
import io.strategiz.client.gcpbilling.model.ClickHouseCloudCost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

/**
 * Client for retrieving ClickHouse Cloud billing data via the official API.
 *
 * API Documentation: https://clickhouse.com/docs/en/cloud/manage/billing/api Endpoint:
 * GET https://api.clickhouse.cloud/v1/organizations/{orgId}/usageCost Auth: HTTP Basic
 * Auth (API Key ID : API Key Secret)
 *
 * Note: Maximum query window is 31 days.
 *
 * Enable with: clickhouse.billing.enabled=true
 */
@Component
@ConditionalOnProperty(name = "clickhouse.billing.enabled", havingValue = "true", matchIfMissing = false)
public class ClickHouseBillingClient {

	private static final Logger log = LoggerFactory.getLogger(ClickHouseBillingClient.class);

	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

	private static final int MAX_QUERY_DAYS = 31;

	private final RestTemplate restTemplate;

	private final ObjectMapper objectMapper;

	private final ClickHouseBillingProperties properties;

	public ClickHouseBillingClient(RestTemplate restTemplate, ObjectMapper objectMapper,
			ClickHouseBillingProperties properties) {
		this.restTemplate = restTemplate;
		this.objectMapper = objectMapper;
		this.properties = properties;
		log.info("ClickHouseBillingClient initialized - Configured: {}", properties.configured());
	}

	/**
	 * Get cost summary for a date range from ClickHouse Cloud API.
	 * @param startDate Start date (inclusive)
	 * @param endDate End date (inclusive)
	 * @return ClickHouse Cloud cost data with breakdown
	 */
	@Cacheable(value = "clickhouseCloudCosts", key = "#startDate.toString() + '-' + #endDate.toString()")
	public ClickHouseCloudCost getCostSummary(LocalDate startDate, LocalDate endDate) {
		log.info("Fetching ClickHouse Cloud cost summary from {} to {}", startDate, endDate);

		if (!properties.configured()) {
			log.warn("ClickHouse Cloud billing not configured - returning zero costs");
			return ClickHouseCloudCost.empty(startDate, endDate);
		}

		// Enforce max 31 day window
		long daysBetween = ChronoUnit.DAYS.between(startDate, endDate);
		if (daysBetween > MAX_QUERY_DAYS) {
			log.warn("Date range exceeds {} days ({} days), truncating to last {} days", MAX_QUERY_DAYS, daysBetween,
					MAX_QUERY_DAYS);
			startDate = endDate.minusDays(MAX_QUERY_DAYS);
		}

		try {
			String url = String.format("%s?from_date=%s&to_date=%s", properties.getUsageCostUrl(),
					startDate.format(DATE_FORMAT), endDate.format(DATE_FORMAT));

			HttpHeaders headers = new HttpHeaders();
			headers.set(HttpHeaders.AUTHORIZATION, createBasicAuthHeader());
			headers.setContentType(MediaType.APPLICATION_JSON);

			HttpEntity<String> entity = new HttpEntity<>(headers);
			ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

			if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
				return parseCostResponse(response.getBody(), startDate, endDate);
			}

			log.warn("Unexpected response from ClickHouse Cloud API: {}", response.getStatusCode());
			return ClickHouseCloudCost.empty(startDate, endDate);

		}
		catch (Exception e) {
			log.error("Error fetching ClickHouse Cloud costs: {}", e.getMessage(), e);
			return ClickHouseCloudCost.empty(startDate, endDate);
		}
	}

	/**
	 * Get current month costs
	 */
	@Cacheable(value = "clickhouseCloudCurrentMonth", key = "'current-month'")
	public ClickHouseCloudCost getCurrentMonthCosts() {
		LocalDate today = LocalDate.now();
		LocalDate startOfMonth = today.withDayOfMonth(1);
		return getCostSummary(startOfMonth, today);
	}

	/**
	 * Create HTTP Basic Auth header from API key credentials
	 */
	private String createBasicAuthHeader() {
		String credentials = properties.keyId() + ":" + properties.keySecret();
		String encoded = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
		return "Basic " + encoded;
	}

	/**
	 * Parse the ClickHouse Cloud API cost response.
	 *
	 * Example response: { "result": { "storageCHC": "0.50", "computeCHC": "2.30",
	 * "backupCHC": "0.10", "dataTransferCHC": "0.05" } }
	 */
	private ClickHouseCloudCost parseCostResponse(String jsonResponse, LocalDate startDate, LocalDate endDate) {
		try {
			JsonNode root = objectMapper.readTree(jsonResponse);

			BigDecimal storageCost = BigDecimal.ZERO;
			BigDecimal computeCost = BigDecimal.ZERO;
			BigDecimal backupCost = BigDecimal.ZERO;
			BigDecimal dataTransferCost = BigDecimal.ZERO;

			// Parse the result object
			JsonNode result = root.has("result") ? root.get("result") : root;

			if (result.has("storageCHC")) {
				storageCost = parseCostValue(result.get("storageCHC"));
			}
			if (result.has("computeCHC")) {
				computeCost = parseCostValue(result.get("computeCHC"));
			}
			if (result.has("backupCHC")) {
				backupCost = parseCostValue(result.get("backupCHC"));
			}
			if (result.has("dataTransferCHC")) {
				dataTransferCost = parseCostValue(result.get("dataTransferCHC"));
			}

			BigDecimal totalCost = storageCost.add(computeCost).add(backupCost).add(dataTransferCost);

			log.info("Parsed ClickHouse Cloud costs: total=${}, compute=${}, storage=${}, backup=${}, transfer=${}",
					totalCost, computeCost, storageCost, backupCost, dataTransferCost);

			return new ClickHouseCloudCost(startDate, endDate, totalCost, computeCost, storageCost, backupCost,
					dataTransferCost, "USD" // CHC = USD 1:1
			);

		}
		catch (Exception e) {
			log.error("Error parsing ClickHouse Cloud cost response: {}", e.getMessage());
			return ClickHouseCloudCost.empty(startDate, endDate);
		}
	}

	/**
	 * Parse a cost value which may be a string or number
	 */
	private BigDecimal parseCostValue(JsonNode node) {
		if (node == null || node.isNull()) {
			return BigDecimal.ZERO;
		}
		if (node.isTextual()) {
			try {
				return new BigDecimal(node.asText());
			}
			catch (NumberFormatException e) {
				return BigDecimal.ZERO;
			}
		}
		if (node.isNumber()) {
			return BigDecimal.valueOf(node.asDouble());
		}
		return BigDecimal.ZERO;
	}

	/**
	 * Check if the client is properly configured
	 */
	public boolean isConfigured() {
		return properties.configured();
	}

}
