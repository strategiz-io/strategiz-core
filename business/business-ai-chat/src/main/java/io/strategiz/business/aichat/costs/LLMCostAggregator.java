package io.strategiz.business.aichat.costs;

import io.strategiz.business.aichat.costs.model.LLMCostSummary;
import io.strategiz.business.aichat.costs.model.ModelCostBreakdown;
import io.strategiz.business.aichat.costs.model.ProviderCostReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Aggregates cost data from all LLM billing providers.
 *
 * This service combines data from OpenAI, Anthropic, GCP (Vertex AI), and xAI to provide
 * a unified view of LLM costs for the console dashboard.
 */
@Service
public class LLMCostAggregator {

	private static final Logger logger = LoggerFactory.getLogger(LLMCostAggregator.class);

	private static final int TOP_MODELS_LIMIT = 10;

	private final List<BillingProvider> billingProviders;

	public LLMCostAggregator(List<BillingProvider> billingProviders) {
		this.billingProviders = billingProviders;
		logger.info("LLMCostAggregator initialized with {} billing providers: {}", billingProviders.size(),
				billingProviders.stream().map(BillingProvider::getProviderName).collect(Collectors.joining(", ")));
	}

	/**
	 * Get a summary of LLM costs for the specified date range. Includes comparison with
	 * the previous period of the same length.
	 * @param startDate Start of the date range (inclusive)
	 * @param endDate End of the date range (inclusive)
	 * @return Aggregated cost summary with provider and model breakdowns
	 */
	public Mono<LLMCostSummary> getCostSummary(LocalDate startDate, LocalDate endDate) {
		logger.info("Fetching cost summary from {} to {}", startDate, endDate);

		// Calculate previous period dates for comparison
		long periodDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;
		LocalDate previousStartDate = startDate.minusDays(periodDays);
		LocalDate previousEndDate = startDate.minusDays(1);

		// Fetch current and previous period costs in parallel
		Mono<List<ProviderCostReport>> currentPeriodMono = fetchAllProviderCosts(startDate, endDate);
		Mono<List<ProviderCostReport>> previousPeriodMono = fetchAllProviderCosts(previousStartDate, previousEndDate);

		return Mono.zip(currentPeriodMono, previousPeriodMono)
			.map(tuple -> buildCostSummary(tuple.getT1(), tuple.getT2(), startDate, endDate));
	}

	/**
	 * Get cost summary for the current month
	 */
	public Mono<LLMCostSummary> getCurrentMonthCosts() {
		LocalDate now = LocalDate.now();
		LocalDate startOfMonth = now.withDayOfMonth(1);
		return getCostSummary(startOfMonth, now);
	}

	/**
	 * Get cost summary for the last N days
	 */
	public Mono<LLMCostSummary> getLastNDaysCosts(int days) {
		LocalDate endDate = LocalDate.now();
		LocalDate startDate = endDate.minusDays(days - 1);
		return getCostSummary(startDate, endDate);
	}

	/**
	 * Get detailed cost breakdown by provider
	 */
	public Mono<List<ProviderCostReport>> getCostsByProvider(LocalDate startDate, LocalDate endDate) {
		return fetchAllProviderCosts(startDate, endDate);
	}

	/**
	 * Get detailed cost breakdown by model across all providers
	 */
	public Mono<List<ModelCostBreakdown>> getCostsByModel(LocalDate startDate, LocalDate endDate) {
		return fetchAllProviderCosts(startDate, endDate).map(reports -> {
			List<ModelCostBreakdown> allModels = new ArrayList<>();
			for (ProviderCostReport report : reports) {
				if (report.getByModel() != null) {
					allModels.addAll(report.getByModel());
				}
			}
			// Sort by cost descending
			allModels.sort(Comparator.comparing(ModelCostBreakdown::getCost).reversed());
			return allModels;
		});
	}

	/**
	 * Fetch costs from all enabled billing providers in parallel
	 */
	private Mono<List<ProviderCostReport>> fetchAllProviderCosts(LocalDate startDate, LocalDate endDate) {
		List<BillingProvider> enabledProviders = billingProviders.stream()
			.filter(BillingProvider::isEnabled)
			.collect(Collectors.toList());

		if (enabledProviders.isEmpty()) {
			logger.warn("No billing providers are enabled");
			return Mono.just(new ArrayList<>());
		}

		logger.debug("Fetching costs from {} enabled providers", enabledProviders.size());

		return Flux.fromIterable(enabledProviders)
			.flatMap(provider -> provider.fetchCosts(startDate, endDate)
				.doOnSuccess(report -> logger.debug("Fetched costs from {}: ${}", provider.getProviderName(),
						report.getTotalCost()))
				.doOnError(error -> logger.error("Error fetching costs from {}", provider.getProviderName(), error))
				.onErrorResume(
						error -> Mono.just(new ProviderCostReport(provider.getProviderName(), startDate, endDate))))
			.collectList();
	}

	/**
	 * Build the cost summary from provider reports
	 */
	private LLMCostSummary buildCostSummary(List<ProviderCostReport> currentReports,
			List<ProviderCostReport> previousReports, LocalDate startDate, LocalDate endDate) {

		LLMCostSummary summary = new LLMCostSummary();
		summary.setStartDate(startDate);
		summary.setEndDate(endDate);

		// Aggregate current period totals
		BigDecimal totalCost = BigDecimal.ZERO;
		long totalTokens = 0;
		long totalRequests = 0;
		Map<String, BigDecimal> costByProvider = new HashMap<>();
		List<ModelCostBreakdown> allModels = new ArrayList<>();

		for (ProviderCostReport report : currentReports) {
			if (report.getTotalCost() != null) {
				totalCost = totalCost.add(report.getTotalCost());
			}
			totalTokens += report.getTotalTokens();
			totalRequests += report.getRequestCount();

			costByProvider.put(report.getProvider(),
					report.getTotalCost() != null ? report.getTotalCost() : BigDecimal.ZERO);

			if (report.getByModel() != null) {
				allModels.addAll(report.getByModel());
			}
		}

		summary.setTotalCost(totalCost);
		summary.setTotalTokens(totalTokens);
		summary.setTotalRequests(totalRequests);
		summary.setCostByProvider(costByProvider);

		// Calculate averages
		if (totalRequests > 0) {
			summary
				.setAverageCostPerRequest(totalCost.divide(BigDecimal.valueOf(totalRequests), 6, RoundingMode.HALF_UP));
		}
		else {
			summary.setAverageCostPerRequest(BigDecimal.ZERO);
		}

		if (totalTokens > 0) {
			summary.setAverageCostPer1kTokens(totalCost.multiply(BigDecimal.valueOf(1000))
				.divide(BigDecimal.valueOf(totalTokens), 6, RoundingMode.HALF_UP));
		}
		else {
			summary.setAverageCostPer1kTokens(BigDecimal.ZERO);
		}

		// Find top models
		allModels
			.sort(Comparator.comparing((ModelCostBreakdown m) -> m.getCost() != null ? m.getCost() : BigDecimal.ZERO)
				.reversed());
		summary.setTopModels(allModels.stream().limit(TOP_MODELS_LIMIT).collect(Collectors.toList()));

		if (!allModels.isEmpty() && allModels.get(0).getCost() != null) {
			summary.setTopModel(allModels.get(0).getModelId());
			summary.setTopModelCost(allModels.get(0).getCost());
		}

		// Calculate previous period total for comparison
		BigDecimal previousTotalCost = BigDecimal.ZERO;
		for (ProviderCostReport report : previousReports) {
			if (report.getTotalCost() != null) {
				previousTotalCost = previousTotalCost.add(report.getTotalCost());
			}
		}
		summary.setPreviousPeriodCost(previousTotalCost);

		// Calculate percentage change
		if (previousTotalCost.compareTo(BigDecimal.ZERO) > 0) {
			BigDecimal change = totalCost.subtract(previousTotalCost)
				.divide(previousTotalCost, 4, RoundingMode.HALF_UP)
				.multiply(BigDecimal.valueOf(100));
			summary.setCostChange(change);
		}
		else if (totalCost.compareTo(BigDecimal.ZERO) > 0) {
			summary.setCostChange(BigDecimal.valueOf(100)); // 100% increase from zero
		}
		else {
			summary.setCostChange(BigDecimal.ZERO);
		}

		logger.info("Cost summary: ${} total, {} tokens, {} requests, {} change from previous period", totalCost,
				totalTokens, totalRequests, summary.getCostChange() + "%");

		return summary;
	}

	/**
	 * Get list of enabled billing providers
	 */
	public List<String> getEnabledProviders() {
		return billingProviders.stream()
			.filter(BillingProvider::isEnabled)
			.map(BillingProvider::getProviderName)
			.collect(Collectors.toList());
	}

	/**
	 * Check if any billing provider is enabled
	 */
	public boolean hasBillingEnabled() {
		return billingProviders.stream().anyMatch(BillingProvider::isEnabled);
	}

}
