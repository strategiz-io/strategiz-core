package io.strategiz.client.gcpbilling;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.strategiz.client.gcpbilling.config.GcpBillingConfig.GcpBillingProperties;
import io.strategiz.client.gcpbilling.model.AiProviderCost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Client for retrieving xAI Grok API costs and usage data.
 * Uses xAI Console API for usage tracking.
 *
 * API Documentation: https://docs.x.ai
 * Required: xAI API key
 *
 * Enable with: ai.billing.enabled=true
 *
 * Note: xAI may not have a public billing API yet. This client will attempt
 * to track usage based on API calls and estimate costs based on published pricing.
 */
@Component
@ConditionalOnProperty(name = "ai.billing.enabled", havingValue = "true", matchIfMissing = false)
public class XAiBillingClient {

    private static final Logger log = LoggerFactory.getLogger(XAiBillingClient.class);
    private static final String API_BASE_URL = "https://api.x.ai/v1";

    // Grok pricing (as of 2025)
    private static final BigDecimal GROK_4_INPUT_PRICE = new BigDecimal("3.00"); // per million tokens
    private static final BigDecimal GROK_4_OUTPUT_PRICE = new BigDecimal("15.00"); // per million tokens
    private static final BigDecimal GROK_4_MINI_INPUT_PRICE = new BigDecimal("0.30");
    private static final BigDecimal GROK_4_MINI_OUTPUT_PRICE = new BigDecimal("0.50");

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String apiKey;

    public XAiBillingClient(RestTemplate restTemplate, ObjectMapper objectMapper, GcpBillingProperties properties) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        // xAI API key should be stored in Vault at secret/strategiz/ai/xai/api-key
        this.apiKey = System.getenv("XAI_API_KEY");
        log.info("XAiBillingClient initialized");
    }

    /**
     * Get estimated cost report based on usage tracking
     *
     * Note: If xAI doesn't provide a billing API, this will estimate costs
     * based on token usage and published pricing.
     *
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return AI provider cost data
     */
    @Cacheable(value = "xaiCosts", key = "#startDate.toString() + '-' + #endDate.toString()")
    public AiProviderCost getCostReport(LocalDate startDate, LocalDate endDate) {
        log.info("Fetching xAI cost estimate from {} to {}", startDate, endDate);

        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("xAI API key not configured");
            return AiProviderCost.empty("xAI Grok", startDate, endDate);
        }

        try {
            // Attempt to get usage data from xAI console
            // This endpoint may not exist - xAI's billing API is not fully documented
            String url = String.format("%s/usage?start_date=%s&end_date=%s",
                    API_BASE_URL, startDate, endDate);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(apiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return parseUsageAndEstimateCost(response.getBody(), startDate, endDate);
            }

            log.info("xAI billing API not available, returning estimated costs based on local tracking");
            return estimateCostFromLocalTracking(startDate, endDate);

        } catch (Exception e) {
            log.debug("xAI billing API not available (expected): {}", e.getMessage());
            return estimateCostFromLocalTracking(startDate, endDate);
        }
    }

    private AiProviderCost parseUsageAndEstimateCost(String jsonResponse, LocalDate startDate, LocalDate endDate) {
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);

            long totalInputTokens = 0;
            long totalOutputTokens = 0;
            Map<String, BigDecimal> costByModel = new HashMap<>();
            long totalRequests = 0;

            // Parse usage data if available
            if (root.has("usage")) {
                JsonNode usage = root.get("usage");
                if (usage.has("input_tokens")) {
                    totalInputTokens = usage.get("input_tokens").asLong();
                }
                if (usage.has("output_tokens")) {
                    totalOutputTokens = usage.get("output_tokens").asLong();
                }
                if (usage.has("requests")) {
                    totalRequests = usage.get("requests").asLong();
                }
            }

            // Estimate costs based on pricing
            BigDecimal inputCost = BigDecimal.valueOf(totalInputTokens)
                    .multiply(GROK_4_INPUT_PRICE)
                    .divide(BigDecimal.valueOf(1_000_000), 4, java.math.RoundingMode.HALF_UP);

            BigDecimal outputCost = BigDecimal.valueOf(totalOutputTokens)
                    .multiply(GROK_4_OUTPUT_PRICE)
                    .divide(BigDecimal.valueOf(1_000_000), 4, java.math.RoundingMode.HALF_UP);

            BigDecimal totalCost = inputCost.add(outputCost);

            costByModel.put("grok-4", totalCost);

            Map<String, BigDecimal> costByService = Map.of(
                    "completions", totalCost
            );

            log.info("Estimated xAI cost: ${} ({} input tokens, {} output tokens)",
                    totalCost, totalInputTokens, totalOutputTokens);

            return new AiProviderCost(
                    "xAI Grok",
                    startDate,
                    endDate,
                    totalCost,
                    costByModel,
                    totalInputTokens + totalOutputTokens,
                    totalRequests,
                    costByService
            );

        } catch (Exception e) {
            log.error("Error parsing xAI usage data: {}", e.getMessage());
            return AiProviderCost.empty("xAI Grok", startDate, endDate);
        }
    }

    private AiProviderCost estimateCostFromLocalTracking(LocalDate startDate, LocalDate endDate) {
        // Return empty for now - would need to implement local usage tracking
        log.info("Returning empty cost report - local tracking not implemented yet");
        return AiProviderCost.empty("xAI Grok", startDate, endDate);
    }
}
