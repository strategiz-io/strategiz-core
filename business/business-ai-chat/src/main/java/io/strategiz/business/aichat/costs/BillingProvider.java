package io.strategiz.business.aichat.costs;

import io.strategiz.business.aichat.costs.model.ModelCostBreakdown;
import io.strategiz.business.aichat.costs.model.ProviderCostReport;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;

/**
 * Interface for billing/usage data providers. Each LLM provider (OpenAI, Anthropic, GCP)
 * implements this interface to fetch cost and usage data from their respective billing
 * APIs.
 */
public interface BillingProvider {

	/**
	 * Get the provider name (e.g., "openai", "anthropic", "google")
	 */
	String getProviderName();

	/**
	 * Check if this billing provider is enabled and configured
	 */
	boolean isEnabled();

	/**
	 * Fetch aggregated costs for a date range
	 * @param startDate Start of the date range (inclusive)
	 * @param endDate End of the date range (inclusive)
	 * @return Provider cost report with aggregated data
	 */
	Mono<ProviderCostReport> fetchCosts(LocalDate startDate, LocalDate endDate);

	/**
	 * Fetch costs broken down by model for a date range
	 * @param startDate Start of the date range (inclusive)
	 * @param endDate End of the date range (inclusive)
	 * @return List of cost breakdowns per model
	 */
	Mono<List<ModelCostBreakdown>> fetchCostsByModel(LocalDate startDate, LocalDate endDate);

}
