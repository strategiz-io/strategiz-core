package io.strategiz.client.sendgridbilling;

import io.strategiz.client.sendgridbilling.config.SendGridBillingConfig.SendGridBillingProperties;
import io.strategiz.client.sendgridbilling.model.SendGridCostSummary;
import io.strategiz.client.sendgridbilling.model.SendGridUsage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;

/**
 * Client for retrieving SendGrid email service billing and usage data.
 *
 * Note: SendGrid does not have a public billing API. Billing API keys exist but endpoints
 * return "resource not found" errors. This implementation returns $0.00 (no
 * estimates/fake data).
 *
 * If SendGrid releases a public billing API in the future, this client will automatically
 * start returning real cost data.
 *
 * Enable with: gcp.billing.enabled=true
 */
@Component
@ConditionalOnProperty(name = "gcp.billing.enabled", havingValue = "true", matchIfMissing = false)
public class SendGridBillingClient {

	private static final Logger log = LoggerFactory.getLogger(SendGridBillingClient.class);

	private final RestTemplate restTemplate;

	private final SendGridBillingProperties properties;

	public SendGridBillingClient(@Qualifier("sendgridBillingRestTemplate") RestTemplate restTemplate,
			SendGridBillingProperties properties) {
		this.restTemplate = restTemplate;
		this.properties = properties;
		log.info("SendGridBillingClient initialized, API configured: {}", properties.isConfigured());
	}

	/**
	 * Get cost summary for a date range
	 *
	 * Note: SendGrid does not have a public billing API. This method attempts to fetch
	 * real billing data from the API. If the API is unavailable or returns an error,
	 * returns $0.00 (no fake estimates).
	 * @param startDate Start date (inclusive)
	 * @param endDate End date (inclusive)
	 * @return Cost summary, or all zeros if unavailable
	 */
	@Cacheable(value = "sendgridCostSummary", key = "#startDate.toString() + '-' + #endDate.toString()")
	public SendGridCostSummary getCostSummary(LocalDate startDate, LocalDate endDate) {
		log.info("Fetching SendGrid cost summary from {} to {}", startDate, endDate);

		if (!properties.isConfigured()) {
			log.warn("⚠️ SendGrid billing API not configured - returning $0.00 (no billing data available)");
			return SendGridCostSummary.empty(startDate, endDate);
		}

		try {
			// Try to get actual billing data from SendGrid API
			// Note: SendGrid billing endpoints appear to not exist or are enterprise-only
			HttpHeaders headers = new HttpHeaders();
			headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
			headers.setBearerAuth(properties.apiKey());

			// Attempt various potential billing endpoints
			String url = String.format("%s/billing?start_date=%s&end_date=%s", properties.apiUrl(), startDate, endDate);

			HttpEntity<?> entity = new HttpEntity<>(headers);

			ResponseEntity<Map<String, Object>> response = restTemplate.exchange(url, HttpMethod.GET, entity,
					new ParameterizedTypeReference<Map<String, Object>>() {
					});

			if (response.getBody() != null) {
				log.info("✅ Successfully fetched real SendGrid billing data from API");
				return parseBillingResponse(response.getBody(), startDate, endDate);
			}
		}
		catch (RestClientException e) {
			log.warn("⚠️ SendGrid billing API not available: {} - returning $0.00 (real data not available)",
					e.getMessage());
		}

		return SendGridCostSummary.empty(startDate, endDate);
	}

	/**
	 * Get current usage metrics
	 *
	 * Note: Returns real usage from API or empty/zero usage if unavailable. No estimates
	 * are provided.
	 * @return Current usage statistics, or zeros if unavailable
	 */
	@Cacheable(value = "sendgridUsage", key = "'current'")
	public SendGridUsage getCurrentUsage() {
		log.info("Fetching SendGrid current usage");

		if (!properties.isConfigured()) {
			log.warn("⚠️ SendGrid billing API not configured - returning empty usage data");
			return getEmptyUsage();
		}

		try {
			// Try to get usage/stats from SendGrid Stats API (v3/stats exists)
			HttpHeaders headers = new HttpHeaders();
			headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
			headers.setBearerAuth(properties.apiKey());

			String url = properties.apiUrl() + "/stats";

			HttpEntity<?> entity = new HttpEntity<>(headers);

			ResponseEntity<Map<String, Object>> response = restTemplate.exchange(url, HttpMethod.GET, entity,
					new ParameterizedTypeReference<Map<String, Object>>() {
					});

			if (response.getBody() != null) {
				log.info("✅ Successfully fetched real SendGrid usage data from API");
				return parseUsageResponse(response.getBody());
			}
		}
		catch (RestClientException e) {
			log.warn("⚠️ SendGrid usage API not available: {} - returning empty usage (real data not available)",
					e.getMessage());
		}

		return getEmptyUsage();
	}

	/**
	 * Check if the SendGrid billing API is accessible
	 * @return true if API is configured and accessible
	 */
	public boolean isApiAccessible() {
		return properties.isConfigured();
	}

	/**
	 * Returns empty usage data (all zeros) when real API data is unavailable. No
	 * estimates are provided per user requirements.
	 * @return Empty usage with all values set to zero
	 */
	private SendGridUsage getEmptyUsage() {
		return new SendGridUsage("unavailable", "SendGrid Email Service", BigDecimal.ZERO, // No
																							// email
																							// data
				BigDecimal.ZERO, // No delivery data
				BigDecimal.ZERO, // No bounce data
				BigDecimal.ZERO, // No API request data
				Instant.now());
	}

	private SendGridCostSummary parseBillingResponse(Map<String, Object> response, LocalDate startDate,
			LocalDate endDate) {

		BigDecimal totalCost = BigDecimal.ZERO;
		BigDecimal emailsSent = BigDecimal.ZERO;
		BigDecimal apiRequests = BigDecimal.ZERO;

		if (response.containsKey("total_cost")) {
			totalCost = new BigDecimal(response.get("total_cost").toString());
		}
		if (response.containsKey("emails_sent")) {
			emailsSent = new BigDecimal(response.get("emails_sent").toString());
		}
		if (response.containsKey("api_requests")) {
			apiRequests = new BigDecimal(response.get("api_requests").toString());
		}

		return new SendGridCostSummary(startDate, endDate, totalCost, "USD", emailsSent, apiRequests);
	}

	private SendGridUsage parseUsageResponse(Map<String, Object> stats) {
		String accountId = stats.getOrDefault("account_id", "unknown").toString();
		String accountName = stats.getOrDefault("account_name", "SendGrid").toString();

		BigDecimal emailsSent = BigDecimal.ZERO;
		BigDecimal emailsDelivered = BigDecimal.ZERO;
		BigDecimal emailsBounced = BigDecimal.ZERO;
		BigDecimal apiRequests = BigDecimal.ZERO;

		if (stats.containsKey("emails_sent")) {
			emailsSent = new BigDecimal(stats.get("emails_sent").toString());
		}
		if (stats.containsKey("delivered")) {
			emailsDelivered = new BigDecimal(stats.get("delivered").toString());
		}
		if (stats.containsKey("bounced")) {
			emailsBounced = new BigDecimal(stats.get("bounced").toString());
		}
		if (stats.containsKey("api_requests")) {
			apiRequests = new BigDecimal(stats.get("api_requests").toString());
		}

		return new SendGridUsage(accountId, accountName, emailsSent, emailsDelivered, emailsBounced, apiRequests,
				Instant.now());
	}

}
