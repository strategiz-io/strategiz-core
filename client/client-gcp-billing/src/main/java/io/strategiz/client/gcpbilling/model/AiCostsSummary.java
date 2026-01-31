package io.strategiz.client.gcpbilling.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Summary of AI/LLM costs across all providers.
 *
 * @param startDate Start of the period
 * @param endDate End of the period
 * @param totalCost Total AI costs across all providers
 * @param costByProvider Breakdown by provider (Anthropic, OpenAI, xAI)
 * @param providerDetails Detailed costs per provider
 * @param totalTokens Total tokens consumed across all providers
 * @param totalRequests Total API requests across all providers
 * @param personalDevCosts Personal Claude development account costs
 */
public record AiCostsSummary(LocalDate startDate, LocalDate endDate, BigDecimal totalCost,
		Map<String, BigDecimal> costByProvider, List<AiProviderCost> providerDetails, Long totalTokens,
		Long totalRequests, BigDecimal personalDevCosts) {
	public static AiCostsSummary empty(LocalDate startDate, LocalDate endDate) {
		return new AiCostsSummary(startDate, endDate, BigDecimal.ZERO, java.util.Collections.emptyMap(),
				java.util.Collections.emptyList(), 0L, 0L, BigDecimal.ZERO);
	}
}
