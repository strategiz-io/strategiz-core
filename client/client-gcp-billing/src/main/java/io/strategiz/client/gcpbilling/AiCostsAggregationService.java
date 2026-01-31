package io.strategiz.client.gcpbilling;

import io.strategiz.client.gcpbilling.model.AiCostsSummary;
import io.strategiz.client.gcpbilling.model.AiProviderCost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * Service for aggregating AI/LLM costs across all providers. Combines costs from
 * Anthropic Claude, OpenAI, and xAI Grok.
 *
 * Enable with: ai.billing.enabled=true
 */
@Service
@ConditionalOnProperty(name = "ai.billing.enabled", havingValue = "true", matchIfMissing = false)
public class AiCostsAggregationService {

	private static final Logger log = LoggerFactory.getLogger(AiCostsAggregationService.class);

	private final AnthropicBillingClient anthropicClient;

	private final OpenAiBillingClient openAiClient;

	private final XAiBillingClient xAiClient;

	public AiCostsAggregationService(AnthropicBillingClient anthropicClient, OpenAiBillingClient openAiClient,
			XAiBillingClient xAiClient) {
		this.anthropicClient = anthropicClient;
		this.openAiClient = openAiClient;
		this.xAiClient = xAiClient;
	}

	/**
	 * Get comprehensive AI costs summary across all providers
	 * @param startDate Start date (inclusive)
	 * @param endDate End date (inclusive)
	 * @return Aggregated AI costs summary
	 */
	@Cacheable(value = "aiCostsSummary", key = "#startDate.toString() + '-' + #endDate.toString()")
	public AiCostsSummary getAiCostsSummary(LocalDate startDate, LocalDate endDate) {
		log.info("Aggregating AI costs from {} to {}", startDate, endDate);

		// Fetch costs from all providers in parallel (using @Cacheable for efficiency)
		AiProviderCost anthropicCost = anthropicClient.getCostReport(startDate, endDate);
		AiProviderCost openAiCost = openAiClient.getCostReport(startDate, endDate);
		AiProviderCost xAiCost = xAiClient.getCostReport(startDate, endDate);

		// Personal dev costs (Claude subscription)
		// This should come from configuration or manual entry
		BigDecimal personalDevCosts = getPersonalClaudeCosts(startDate, endDate);

		List<AiProviderCost> providerDetails = List.of(anthropicCost, openAiCost, xAiCost);

		// Aggregate totals
		BigDecimal totalCost = providerDetails.stream()
			.map(AiProviderCost::totalCost)
			.reduce(BigDecimal.ZERO, BigDecimal::add)
			.add(personalDevCosts);

		long totalTokens = providerDetails.stream().mapToLong(AiProviderCost::tokenUsage).sum();

		long totalRequests = providerDetails.stream().mapToLong(AiProviderCost::requestCount).sum();

		// Build cost by provider map
		Map<String, BigDecimal> costByProvider = new HashMap<>();
		costByProvider.put("Anthropic Claude", anthropicCost.totalCost());
		costByProvider.put("OpenAI", openAiCost.totalCost());
		costByProvider.put("xAI Grok", xAiCost.totalCost());
		costByProvider.put("Personal Claude Dev", personalDevCosts);

		log.info("Total AI costs: ${} across {} providers ({} total tokens, {} requests)", totalCost,
				providerDetails.size(), totalTokens, totalRequests);

		return new AiCostsSummary(startDate, endDate, totalCost, costByProvider, providerDetails, totalTokens,
				totalRequests, personalDevCosts);
	}

	/**
	 * Get current month AI costs summary
	 */
	@Cacheable(value = "currentMonthAiCosts", key = "'current-month'")
	public AiCostsSummary getCurrentMonthAiCosts() {
		LocalDate today = LocalDate.now();
		LocalDate startOfMonth = today.withDayOfMonth(1);
		return getAiCostsSummary(startOfMonth, today);
	}

	/**
	 * Get personal Claude development account costs This is a subscription cost, prorated
	 * for the period
	 * @param startDate Start date
	 * @param endDate End date
	 * @return Personal dev costs for the period
	 */
	private BigDecimal getPersonalClaudeCosts(LocalDate startDate, LocalDate endDate) {
		// Claude Pro subscription: $20/month
		// Claude Team subscription: $30/month per user
		// Estimate: $20/month for personal dev account

		BigDecimal monthlySubscription = new BigDecimal("20.00");

		// Calculate days in the period
		long days = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;

		// Get days in the month
		int daysInMonth = startDate.lengthOfMonth();

		// Prorate the cost
		BigDecimal proratedCost = monthlySubscription.multiply(BigDecimal.valueOf(days))
			.divide(BigDecimal.valueOf(daysInMonth), 2, java.math.RoundingMode.HALF_UP);

		log.debug("Personal Claude dev costs: ${} ({} days @ ${}/month)", proratedCost, days, monthlySubscription);

		return proratedCost;
	}

	/**
	 * Get AI costs breakdown by model
	 */
	public Map<String, BigDecimal> getCostsByModel(LocalDate startDate, LocalDate endDate) {
		Map<String, BigDecimal> costsByModel = new HashMap<>();

		AiCostsSummary summary = getAiCostsSummary(startDate, endDate);

		for (AiProviderCost provider : summary.providerDetails()) {
			for (Map.Entry<String, BigDecimal> entry : provider.costByModel().entrySet()) {
				String modelKey = provider.provider() + " - " + entry.getKey();
				costsByModel.put(modelKey, entry.getValue());
			}
		}

		return costsByModel;
	}

	/**
	 * Get daily AI costs for trend analysis
	 */
	public List<Map<String, Object>> getDailyAiCosts(int days) {
		List<Map<String, Object>> dailyCosts = new ArrayList<>();
		LocalDate endDate = LocalDate.now();
		LocalDate startDate = endDate.minusDays(days);

		// For simplicity, we'll return monthly totals
		// In production, you might want to track daily usage
		AiCostsSummary summary = getAiCostsSummary(startDate, endDate);

		BigDecimal avgDailyCost = summary.totalCost()
			.divide(BigDecimal.valueOf(days), 4, java.math.RoundingMode.HALF_UP);

		for (int i = 0; i < days; i++) {
			LocalDate date = endDate.minusDays(i);
			Map<String, Object> dailyCost = new HashMap<>();
			dailyCost.put("date", date.toString());
			dailyCost.put("totalCost", avgDailyCost);
			dailyCost.put("breakdown", summary.costByProvider());
			dailyCosts.add(dailyCost);
		}

		return dailyCosts;
	}

}
