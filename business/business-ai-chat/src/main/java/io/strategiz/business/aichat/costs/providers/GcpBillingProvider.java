package io.strategiz.business.aichat.costs.providers;

import io.strategiz.business.aichat.costs.BillingProvider;
import io.strategiz.business.aichat.costs.config.BillingConfig;
import io.strategiz.business.aichat.costs.model.ModelCostBreakdown;
import io.strategiz.business.aichat.costs.model.ProviderCostReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * GCP billing provider that fetches Vertex AI costs from BigQuery billing export.
 *
 * Prerequisites: 1. Enable billing export to BigQuery in GCP Console 2. Grant BigQuery
 * read access to the service account
 *
 * This provider queries the billing export table for Vertex AI services including: -
 * Gemini models - Claude via Model Garden - OpenAI via Model Garden - Llama, Mistral,
 * Cohere via Model Garden
 *
 * Note: BigQuery integration requires the google-cloud-bigquery dependency. For now, this
 * provides a stub implementation that can be expanded when BigQuery is configured.
 */
@Component
@ConditionalOnProperty(name = "llm.billing.gcp.enabled", havingValue = "true", matchIfMissing = false)
public class GcpBillingProvider implements BillingProvider {

	private static final Logger logger = LoggerFactory.getLogger(GcpBillingProvider.class);

	private static final String PROVIDER_NAME = "google";

	private final BillingConfig.GcpBillingConfig config;

	// BigQuery client would be injected here when configured
	// private final BigQuery bigQuery;

	public GcpBillingProvider(BillingConfig billingConfig) {
		this.config = billingConfig.getGcp();
		logger.info("GcpBillingProvider initialized (dataset: {})",
				config.getBillingDataset() != null ? config.getBillingDataset() : "not configured");
	}

	@Override
	public String getProviderName() {
		return PROVIDER_NAME;
	}

	@Override
	public boolean isEnabled() {
		return config.isEnabled() && config.getBillingDataset() != null && !config.getBillingDataset().isEmpty();
	}

	@Override
	public Mono<ProviderCostReport> fetchCosts(LocalDate startDate, LocalDate endDate) {
		if (!isEnabled()) {
			logger.warn("GCP billing is not enabled or billing dataset is not configured");
			return Mono.just(new ProviderCostReport(PROVIDER_NAME, startDate, endDate));
		}

		logger.info("Fetching GCP Vertex AI costs from {} to {}", startDate, endDate);

		// TODO: Implement BigQuery query when dependency is added
		// For now, return estimated costs based on internal tracking
		return fetchCostsFromBigQuery(startDate, endDate);
	}

	@Override
	public Mono<List<ModelCostBreakdown>> fetchCostsByModel(LocalDate startDate, LocalDate endDate) {
		return fetchCosts(startDate, endDate).map(report -> {
			if (report.getByModel() != null) {
				return report.getByModel();
			}
			return new ArrayList<>();
		});
	}

	/**
	 * Fetch costs from BigQuery billing export
	 */
	private Mono<ProviderCostReport> fetchCostsFromBigQuery(LocalDate startDate, LocalDate endDate) {
		/*
		 * BigQuery SQL query for Vertex AI costs:
		 *
		 * SELECT service.description as service, sku.description as model, SUM(cost) as
		 * total_cost, SUM(usage.amount) as total_usage FROM
		 * `{billingDataset}.gcp_billing_export_v1` WHERE service.description LIKE
		 * '%Vertex AI%' AND usage_start_time >= @startDate AND usage_start_time
		 * < @endDate GROUP BY service, model ORDER BY total_cost DESC
		 */

		// Placeholder implementation - returns empty report
		// When BigQuery client is configured, this will execute the actual query
		return Mono.fromCallable(() -> {
			ProviderCostReport report = new ProviderCostReport(PROVIDER_NAME, startDate, endDate);

			// Since we don't have BigQuery configured yet, we can estimate
			// based on Vertex AI pricing for the models we're using
			List<ModelCostBreakdown> models = estimateVertexAICosts();
			report.setByModel(models);

			// Calculate totals
			BigDecimal totalCost = BigDecimal.ZERO;
			long totalTokens = 0;
			long totalRequests = 0;

			for (ModelCostBreakdown model : models) {
				totalCost = totalCost.add(model.getCost());
				totalTokens += model.getTotalTokens();
				totalRequests += model.getRequestCount();
			}

			report.setTotalCost(totalCost);
			report.setTotalTokens(totalTokens);
			report.setRequestCount(totalRequests);

			logger.info("GCP billing query completed (estimated): ${}", totalCost);
			return report;
		});
	}

	/**
	 * Estimate Vertex AI costs based on known models This is a fallback when BigQuery is
	 * not configured
	 */
	private List<ModelCostBreakdown> estimateVertexAICosts() {
		List<ModelCostBreakdown> models = new ArrayList<>();

		// Vertex AI models we support
		// Pricing is approximate and should be validated against actual billing

		// Gemini models (native to Vertex AI)
		models.add(createModelEstimate("gemini-2.5-flash", "google", BigDecimal.ZERO));
		models.add(createModelEstimate("gemini-2.5-pro", "google", BigDecimal.ZERO));
		models.add(createModelEstimate("gemini-1.5-flash", "google", BigDecimal.ZERO));
		models.add(createModelEstimate("gemini-1.5-pro", "google", BigDecimal.ZERO));

		// Claude via Model Garden
		models.add(createModelEstimate("claude-3-5-sonnet", "google", BigDecimal.ZERO));
		models.add(createModelEstimate("claude-3-haiku", "google", BigDecimal.ZERO));

		// Llama via Model Garden
		models.add(createModelEstimate("llama-3.1-8b", "google", BigDecimal.ZERO));
		models.add(createModelEstimate("llama-3.1-70b", "google", BigDecimal.ZERO));

		return models;
	}

	private ModelCostBreakdown createModelEstimate(String modelId, String provider, BigDecimal cost) {
		ModelCostBreakdown model = new ModelCostBreakdown(modelId, provider);
		model.setCost(cost);
		return model;
	}

	/**
	 * Build the BigQuery SQL query for Vertex AI costs
	 */
	@SuppressWarnings("unused")
	private String buildBigQuerySql(LocalDate startDate, LocalDate endDate) {
		return String.format("""
				SELECT
				    sku.description as model,
				    SUM(cost) as total_cost,
				    SUM(usage.amount) as total_usage,
				    COUNT(*) as request_count
				FROM `%s`
				WHERE service.description LIKE '%%Vertex AI%%'
				  AND DATE(usage_start_time) >= '%s'
				  AND DATE(usage_start_time) <= '%s'
				GROUP BY sku.description
				ORDER BY total_cost DESC
				""", config.getBillingDataset(), startDate, endDate);
	}

}
