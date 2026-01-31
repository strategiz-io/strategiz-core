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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Anthropic billing provider that fetches usage and cost data from the Anthropic Admin
 * API.
 *
 * API Documentation: https://docs.anthropic.com/en/api/usage-cost-api
 *
 * Endpoints used: - GET /v1/organizations/usage_report/messages - Usage data with token
 * breakdown - GET /v1/organizations/cost_report - Cost data in USD
 */
@Component
@ConditionalOnProperty(name = "llm.billing.anthropic.enabled", havingValue = "true", matchIfMissing = false)
public class AnthropicBillingProvider implements BillingProvider {

	private static final Logger logger = LoggerFactory.getLogger(AnthropicBillingProvider.class);

	private static final String PROVIDER_NAME = "anthropic";

	private static final String API_VERSION = "2023-06-01";

	private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

	private final BillingConfig.AnthropicBillingConfig config;

	private final ObjectMapper objectMapper;

	private final WebClient webClient;

	public AnthropicBillingProvider(BillingConfig billingConfig) {
		this.config = billingConfig.getAnthropic();
		this.objectMapper = new ObjectMapper();

		this.webClient = WebClient.builder()
			.baseUrl(config.getApiUrl())
			.defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
			.defaultHeader("x-api-key", config.getAdminApiKey())
			.defaultHeader("anthropic-version", API_VERSION)
			.build();

		logger.info("AnthropicBillingProvider initialized");
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
			logger.warn("Anthropic billing is not enabled or admin API key is missing");
			return Mono.just(new ProviderCostReport(PROVIDER_NAME, startDate, endDate));
		}

		logger.info("Fetching Anthropic costs from {} to {}", startDate, endDate);

		// Format dates as ISO-8601
		String startDateStr = startDate.atStartOfDay() + "Z";
		String endDateStr = endDate.plusDays(1).atStartOfDay() + "Z";

		// Fetch usage data
		return webClient.get()
			.uri(uriBuilder -> uriBuilder.path("/v1/organizations/usage_report/messages")
				.queryParam("starting_at", startDateStr)
				.queryParam("ending_at", endDateStr)
				.queryParam("bucket_width", "1d")
				.queryParam("group_by", "model")
				.build())
			.retrieve()
			.bodyToMono(String.class)
			.timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
			.flatMap(responseBody -> parseUsageResponse(responseBody, startDate, endDate))
			.doOnSuccess(report -> logger.info("Successfully fetched Anthropic costs: ${}", report.getTotalCost()))
			.doOnError(error -> logger.error("Error fetching Anthropic costs", error))
			.onErrorResume(error -> {
				logger.error("Failed to fetch Anthropic costs, returning empty report", error);
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
	 * Parse the Anthropic usage API response
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
					// Each bucket is a time period with usage data
					String model = bucket.has("model") ? bucket.get("model").asText() : "unknown";

					// Anthropic provides different token types
					long inputTokens = bucket.has("input_tokens") ? bucket.get("input_tokens").asLong() : 0;
					long outputTokens = bucket.has("output_tokens") ? bucket.get("output_tokens").asLong() : 0;
					long cachedInputTokens = bucket.has("cache_read_input_tokens")
							? bucket.get("cache_read_input_tokens").asLong() : 0;
					long cacheCreationTokens = bucket.has("cache_creation_input_tokens")
							? bucket.get("cache_creation_input_tokens").asLong() : 0;
					long requests = bucket.has("num_messages") ? bucket.get("num_messages").asLong() : 0;

					// Accumulate to report totals
					report.setInputTokens(report.getInputTokens() + inputTokens);
					report.setOutputTokens(report.getOutputTokens() + outputTokens);
					report.setCachedTokens(report.getCachedTokens() + cachedInputTokens);
					report.setRequestCount(report.getRequestCount() + requests);

					// Accumulate to model breakdown
					ModelCostBreakdown modelBreakdown = modelMap.computeIfAbsent(model, m -> {
						ModelCostBreakdown mb = new ModelCostBreakdown(m, PROVIDER_NAME);
						modelBreakdowns.add(mb);
						return mb;
					});
					modelBreakdown.setInputTokens(modelBreakdown.getInputTokens() + inputTokens);
					modelBreakdown.setOutputTokens(modelBreakdown.getOutputTokens() + outputTokens);
					modelBreakdown.setCachedTokens(modelBreakdown.getCachedTokens() + cachedInputTokens);
					modelBreakdown.setRequestCount(modelBreakdown.getRequestCount() + requests);
				}
			}

			report.setTotalTokens(report.getInputTokens() + report.getOutputTokens());
			report.setByModel(modelBreakdowns);

			// Fetch actual cost data
			return fetchCostData(startDate, endDate, report);
		}
		catch (Exception e) {
			logger.error("Error parsing Anthropic usage response", e);
			return Mono.just(new ProviderCostReport(PROVIDER_NAME, startDate, endDate));
		}
	}

	/**
	 * Fetch cost data from the Anthropic cost report endpoint
	 */
	private Mono<ProviderCostReport> fetchCostData(LocalDate startDate, LocalDate endDate,
			ProviderCostReport usageReport) {
		String startDateStr = startDate.atStartOfDay() + "Z";
		String endDateStr = endDate.plusDays(1).atStartOfDay() + "Z";

		return webClient.get()
			.uri(uriBuilder -> uriBuilder.path("/v1/organizations/cost_report")
				.queryParam("starting_at", startDateStr)
				.queryParam("ending_at", endDateStr)
				.build())
			.retrieve()
			.bodyToMono(String.class)
			.timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
			.map(responseBody -> parseCostResponse(responseBody, usageReport))
			.onErrorResume(error -> {
				logger.warn("Failed to fetch Anthropic cost data, estimating from usage", error);
				// Estimate costs based on published pricing if cost API fails
				estimateCostsFromUsage(usageReport);
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
				for (JsonNode costEntry : root.get("data")) {
					// Anthropic returns costs as decimal strings in cents
					if (costEntry.has("cost_cents")) {
						String costCents = costEntry.get("cost_cents").asText();
						BigDecimal cents = new BigDecimal(costCents);
						BigDecimal dollars = cents.divide(BigDecimal.valueOf(100), 6, java.math.RoundingMode.HALF_UP);
						totalCost = totalCost.add(dollars);
					}
				}
			}

			usageReport.setTotalCost(totalCost);

			// Distribute costs to models based on token usage
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
			logger.error("Error parsing Anthropic cost response", e);
			estimateCostsFromUsage(usageReport);
			return usageReport;
		}
	}

	/**
	 * Estimate costs from usage based on published Anthropic pricing
	 */
	private void estimateCostsFromUsage(ProviderCostReport report) {
		// Anthropic pricing (approximate, per 1M tokens as of 2025)
		// Claude 4.5 Opus: $15 input, $75 output
		// Claude 4.5 Sonnet: $3 input, $15 output
		// Claude 4.5 Haiku: $0.25 input, $1.25 output

		// Use average pricing for estimation
		BigDecimal avgInputPricePerM = new BigDecimal("3.00"); // $3 per 1M input
		BigDecimal avgOutputPricePerM = new BigDecimal("15.00"); // $15 per 1M output

		BigDecimal inputCost = avgInputPricePerM.multiply(BigDecimal.valueOf(report.getInputTokens()))
			.divide(BigDecimal.valueOf(1_000_000), 6, java.math.RoundingMode.HALF_UP);

		BigDecimal outputCost = avgOutputPricePerM.multiply(BigDecimal.valueOf(report.getOutputTokens()))
			.divide(BigDecimal.valueOf(1_000_000), 6, java.math.RoundingMode.HALF_UP);

		report.setTotalCost(inputCost.add(outputCost));

		// Apply same estimation to models
		if (report.getByModel() != null) {
			for (ModelCostBreakdown model : report.getByModel()) {
				BigDecimal modelInputCost = avgInputPricePerM.multiply(BigDecimal.valueOf(model.getInputTokens()))
					.divide(BigDecimal.valueOf(1_000_000), 6, java.math.RoundingMode.HALF_UP);
				BigDecimal modelOutputCost = avgOutputPricePerM.multiply(BigDecimal.valueOf(model.getOutputTokens()))
					.divide(BigDecimal.valueOf(1_000_000), 6, java.math.RoundingMode.HALF_UP);
				model.setCost(modelInputCost.add(modelOutputCost));
			}
		}
	}

}
