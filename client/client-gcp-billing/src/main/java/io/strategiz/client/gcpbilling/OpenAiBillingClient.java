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
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

/**
 * Client for retrieving OpenAI API costs and usage data.
 * Uses OpenAI's official Costs API.
 *
 * API Documentation: https://platform.openai.com/docs/api-reference/usage
 * Required: OpenAI API key with organization access
 *
 * Credentials loaded from Vault: secret/strategiz/ai/openai
 * Required secrets: api-key
 *
 * Enable with: ai.billing.enabled=true
 */
@Component
@ConditionalOnProperty(name = "ai.billing.enabled", havingValue = "true", matchIfMissing = false)
public class OpenAiBillingClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiBillingClient.class);
    private static final String API_BASE_URL = "https://api.openai.com/v1";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String apiKey;

    public OpenAiBillingClient(RestTemplate restTemplate, ObjectMapper objectMapper,
            VaultSecretService vaultSecretService) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;

        // Load API key from Vault: secret/strategiz/ai/openai (api-key field)
        String vaultKey = null;
        try {
            vaultKey = vaultSecretService.readSecret("ai/openai.api-key");
            if (vaultKey == null || vaultKey.isEmpty()) {
                log.warn("API key not found in Vault at ai/openai.api-key, "
                        + "falling back to environment variable");
                vaultKey = System.getenv("OPENAI_API_KEY");
            }
        } catch (Exception e) {
            log.warn("Could not load OpenAI API key from Vault: {}, falling back to environment variable",
                    e.getMessage());
            vaultKey = System.getenv("OPENAI_API_KEY");
        }
        this.apiKey = vaultKey;

        boolean configured = apiKey != null && !apiKey.isEmpty();
        log.info("OpenAiBillingClient initialized - Configured: {}", configured);
    }

    /**
     * Get cost report using OpenAI's Costs API
     *
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return AI provider cost data
     */
    @Cacheable(value = "openaiCosts", key = "#startDate.toString() + '-' + #endDate.toString()")
    public AiProviderCost getCostReport(LocalDate startDate, LocalDate endDate) {
        log.info("Fetching OpenAI cost report from {} to {}", startDate, endDate);

        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("OpenAI API key not configured");
            return AiProviderCost.empty("OpenAI", startDate, endDate);
        }

        try {
            // Convert dates to Unix timestamps
            long startTime = startDate.atStartOfDay(ZoneOffset.UTC).toEpochSecond();
            long endTime = endDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toEpochSecond();

            String url = String.format("%s/organization/costs?start_time=%d&end_time=%d&bucket_width=1d&group_by[]=line_item",
                    API_BASE_URL, startTime, endTime);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(apiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return parseCostReport(response.getBody(), startDate, endDate);
            }

            log.warn("Unexpected response from OpenAI API: {}", response.getStatusCode());
            return AiProviderCost.empty("OpenAI", startDate, endDate);

        } catch (Exception e) {
            log.error("Error fetching OpenAI cost report: {}", e.getMessage(), e);
            return AiProviderCost.empty("OpenAI", startDate, endDate);
        }
    }

    /**
     * Get usage data for token consumption
     */
    @Cacheable(value = "openaiUsage", key = "#startDate.toString() + '-' + #endDate.toString()")
    public Map<String, Long> getUsageData(LocalDate startDate, LocalDate endDate) {
        log.info("Fetching OpenAI usage data from {} to {}", startDate, endDate);

        if (apiKey == null || apiKey.isEmpty()) {
            return Map.of();
        }

        try {
            long startTime = startDate.atStartOfDay(ZoneOffset.UTC).toEpochSecond();
            long endTime = endDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toEpochSecond();

            String url = String.format("%s/organization/usage/completions?start_time=%d&end_time=%d&bucket_width=1d&group_by[]=model",
                    API_BASE_URL, startTime, endTime);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(apiKey);

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return parseUsageData(response.getBody());
            }

        } catch (Exception e) {
            log.error("Error fetching OpenAI usage data: {}", e.getMessage());
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

            // Parse data buckets
            if (root.has("data")) {
                JsonNode data = root.get("data");
                for (JsonNode bucket : data) {
                    if (bucket.has("results")) {
                        for (JsonNode result : bucket.get("results")) {
                            double amount = result.has("amount") ? result.get("amount").asDouble() : 0.0;
                            totalCost = totalCost.add(BigDecimal.valueOf(amount / 100.0)); // Convert cents to dollars

                            // Extract line item (service type)
                            if (result.has("line_item")) {
                                String lineItem = result.get("line_item").asText();
                                costByService.merge(lineItem, BigDecimal.valueOf(amount / 100.0), BigDecimal::add);
                            }
                        }
                    }
                }
            }

            log.info("Parsed OpenAI cost report: ${} total", totalCost);

            return new AiProviderCost(
                    "OpenAI",
                    startDate,
                    endDate,
                    totalCost,
                    costByModel,
                    totalTokens,
                    totalRequests,
                    costByService
            );

        } catch (Exception e) {
            log.error("Error parsing OpenAI cost report: {}", e.getMessage());
            return AiProviderCost.empty("OpenAI", startDate, endDate);
        }
    }

    private Map<String, Long> parseUsageData(String jsonResponse) {
        Map<String, Long> usage = new HashMap<>();
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            if (root.has("data")) {
                long totalInputTokens = 0;
                long totalOutputTokens = 0;
                for (JsonNode bucket : root.get("data")) {
                    if (bucket.has("n_context_tokens_total")) {
                        totalInputTokens += bucket.get("n_context_tokens_total").asLong();
                    }
                    if (bucket.has("n_generated_tokens_total")) {
                        totalOutputTokens += bucket.get("n_generated_tokens_total").asLong();
                    }
                }
                usage.put("input_tokens", totalInputTokens);
                usage.put("output_tokens", totalOutputTokens);
            }
        } catch (Exception e) {
            log.error("Error parsing usage data: {}", e.getMessage());
        }
        return usage;
    }
}
