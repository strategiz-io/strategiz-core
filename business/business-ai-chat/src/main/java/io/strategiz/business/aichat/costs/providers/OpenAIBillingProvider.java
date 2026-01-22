package io.strategiz.business.aichat.costs.providers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.strategiz.business.aichat.costs.BillingProvider;
import io.strategiz.business.aichat.costs.config.BillingConfig;
import io.strategiz.business.aichat.costs.model.ModelCostBreakdown;
import io.strategiz.business.aichat.costs.model.ProviderCostReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAI billing provider that fetches usage and cost data from the OpenAI Admin API.
 *
 * API Documentation: https://platform.openai.com/docs/api-reference/usage
 *
 * Endpoints used: - GET /v1/organization/usage/completions - Usage data grouped by model -
 * GET /v1/organization/costs - Cost data with daily breakdown
 */
@Component
@ConditionalOnProperty(name = "llm.billing.openai.enabled", havingValue = "true", matchIfMissing = false)
public class OpenAIBillingProvider implements BillingProvider {

	private static final Logger logger = LoggerFactory.getLogger(OpenAIBillingProvider.class);

	private static final String PROVIDER_NAME = "openai";

	private final BillingConfig.OpenAIBillingConfig config;

	private final ObjectMapper objectMapper;

	private final WebClient webClient;

	public OpenAIBillingProvider(BillingConfig billingConfig) {
		this.config = billingConfig.getOpenai();
		this.objectMapper = new ObjectMapper();

		this.webClient = WebClient.builder()
			.baseUrl(config.getApiUrl())
			.defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
			.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + config.getAdminApiKey())
			.build();

		logger.info("OpenAIBillingProvider initialized");
	}

	@Override
	public String getProviderName() {
		return PROVIDER_NAME;
	}

	@Override
	public boolean isEnabled() {
		return config.isEnabled() && config.getAdminApiKey() != null && !config.getAdminApiKey().isEmpty();
	}

	@Override
	public Mono<ProviderCostReport> fetchCosts(LocalDate startDate, LocalDate endDate) {
		if (!isEnabled()) {
			logger.warn("OpenAI billing is not enabled or admin API key is missing");
			return Mono.just(new ProviderCostReport(PROVIDER_NAME, startDate, endDate));
		}

		logger.info("Fetching OpenAI costs from {} to {}", startDate, endDate);

		// Convert dates to Unix timestamps
		long startTimestamp = startDate.atStartOfDay(ZoneOffset.UTC).toEpochSecond();
		long endTimestamp = endDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toEpochSecond();

		// Fetch usage data grouped by model
		return webClient.get()
			.uri(uriBuilder -> uriBuilder.path("/v1/organization/usage/completions")
				.queryParam("start_time", startTimestamp)
				.queryParam("end_time", endTimestamp)
				.queryParam("group_by", "model")
				.build())
			.retrieve()
			.bodyToMono(String.class)
			.timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
			.flatMap(responseBody -> parseUsageResponse(responseBody, startDate, endDate))
			.doOnSuccess(report -> logger.info("Successfully fetched OpenAI costs: ${}", report.getTotalCost()))
			.doOnError(error -> logger.error("Error fetching OpenAI costs", error))
			.onErrorResume(error -> {
				logger.error("Failed to fetch OpenAI costs, returning empty report", error);
				return Mono.just(new ProviderCostReport(PROVIDER_NAME, startDate, endDate));
			});
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
	 * Parse the OpenAI usage API response
	 */
	private Mono<ProviderCostReport> parseUsageResponse(String responseBody, LocalDate startDate, LocalDate endDate) {
		try {
			JsonNode root = objectMapper.readTree(responseBody);

			ProviderCostReport report = new ProviderCostReport(PROVIDER_NAME, startDate, endDate);
			List<ModelCostBreakdown> modelBreakdowns = new ArrayList<>();
			Map<String, ModelCostBreakdown> modelMap = new HashMap<>();

			// Parse the data array
			if (root.has("data") && root.get("data").isArray()) {
				for (JsonNode bucket : root.get("data")) {
					// Each bucket contains usage for a time period
					if (bucket.has("results") && bucket.get("results").isArray()) {
						for (JsonNode result : bucket.get("results")) {
							String model = result.has("model") ? result.get("model").asText() : "unknown";

							long inputTokens = result.has("input_tokens") ? result.get("input_tokens").asLong() : 0;
							long outputTokens = result.has("output_tokens") ? result.get("output_tokens").asLong() : 0;
							long cachedTokens = result.has("input_cached_tokens")
									? result.get("input_cached_tokens").asLong() : 0;
							long requests = result.has("num_model_requests")
									? result.get("num_model_requests").asLong() : 0;

							// Accumulate to report totals
							report.setInputTokens(report.getInputTokens() + inputTokens);
							report.setOutputTokens(report.getOutputTokens() + outputTokens);
							report.setCachedTokens(report.getCachedTokens() + cachedTokens);
							report.setRequestCount(report.getRequestCount() + requests);

							// Accumulate to model breakdown
							ModelCostBreakdown modelBreakdown = modelMap.computeIfAbsent(model, m -> {
								ModelCostBreakdown mb = new ModelCostBreakdown(m, PROVIDER_NAME);
								modelBreakdowns.add(mb);
								return mb;
							});
							modelBreakdown.setInputTokens(modelBreakdown.getInputTokens() + inputTokens);
							modelBreakdown.setOutputTokens(modelBreakdown.getOutputTokens() + outputTokens);
							modelBreakdown.setCachedTokens(modelBreakdown.getCachedTokens() + cachedTokens);
							modelBreakdown.setRequestCount(modelBreakdown.getRequestCount() + requests);
						}
					}
				}
			}

			report.setTotalTokens(report.getInputTokens() + report.getOutputTokens());
			report.setByModel(modelBreakdowns);

			// Now fetch actual cost data from the costs endpoint
			return fetchCostData(startDate, endDate, report);
		}
		catch (Exception e) {
			logger.error("Error parsing OpenAI usage response", e);
			return Mono.just(new ProviderCostReport(PROVIDER_NAME, startDate, endDate));
		}
	}

	/**
	 * Fetch cost data from the OpenAI costs endpoint
	 */
	private Mono<ProviderCostReport> fetchCostData(LocalDate startDate, LocalDate endDate,
			ProviderCostReport usageReport) {
		long startTimestamp = startDate.atStartOfDay(ZoneOffset.UTC).toEpochSecond();
		long endTimestamp = endDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toEpochSecond();

		return webClient.get()
			.uri(uriBuilder -> uriBuilder.path("/v1/organization/costs")
				.queryParam("start_time", startTimestamp)
				.queryParam("end_time", endTimestamp)
				.build())
			.retrieve()
			.bodyToMono(String.class)
			.timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
			.map(responseBody -> parseCostResponse(responseBody, usageReport))
			.onErrorResume(error -> {
				logger.warn("Failed to fetch OpenAI cost data, using usage data only", error);
				return Mono.just(usageReport);
			});
	}

	/**
	 * Parse the cost response and merge with usage report
	 */
	private ProviderCostReport parseCostResponse(String responseBody, ProviderCostReport usageReport) {
		try {
			JsonNode root = objectMapper.readTree(responseBody);

			BigDecimal totalCost = BigDecimal.ZERO;

			// Parse the data array for costs
			if (root.has("data") && root.get("data").isArray()) {
				for (JsonNode bucket : root.get("data")) {
					if (bucket.has("results") && bucket.get("results").isArray()) {
						for (JsonNode result : bucket.get("results")) {
							// Cost is in cents, convert to dollars
							if (result.has("amount") && result.has("amount_type")) {
								JsonNode amount = result.get("amount");
								if (amount.has("value")) {
									BigDecimal value = new BigDecimal(amount.get("value").asText());
									totalCost = totalCost.add(value);
								}
							}
						}
					}
				}
			}

			usageReport.setTotalCost(totalCost);

			// Estimate model costs based on token distribution
			if (usageReport.getByModel() != null && usageReport.getTotalTokens() > 0) {
				for (ModelCostBreakdown model : usageReport.getByModel()) {
					double tokenRatio = (double) model.getTotalTokens() / usageReport.getTotalTokens();
					BigDecimal estimatedCost = totalCost.multiply(BigDecimal.valueOf(tokenRatio));
					model.setCost(estimatedCost);
				}
			}

			return usageReport;
		}
		catch (Exception e) {
			logger.error("Error parsing OpenAI cost response", e);
			return usageReport;
		}
	}

}
