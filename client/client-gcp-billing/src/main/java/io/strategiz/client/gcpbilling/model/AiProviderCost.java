package io.strategiz.client.gcpbilling.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * Represents AI/LLM provider costs for a specific period.
 *
 * @param provider Provider name (Anthropic, OpenAI, xAI, etc.)
 * @param startDate Start of the period
 * @param endDate End of the period
 * @param totalCost Total cost in USD
 * @param costByModel Breakdown of costs by model (e.g., gpt-4, claude-opus-4, grok-3)
 * @param tokenUsage Total tokens consumed
 * @param requestCount Total API requests
 * @param costByService Breakdown by service (completions, embeddings, tools, etc.)
 */
public record AiProviderCost(
        String provider,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal totalCost,
        Map<String, BigDecimal> costByModel,
        Long tokenUsage,
        Long requestCount,
        Map<String, BigDecimal> costByService
) {
    public static AiProviderCost empty(String provider, LocalDate startDate, LocalDate endDate) {
        return new AiProviderCost(
                provider,
                startDate,
                endDate,
                BigDecimal.ZERO,
                java.util.Collections.emptyMap(),
                0L,
                0L,
                java.util.Collections.emptyMap()
        );
    }
}
