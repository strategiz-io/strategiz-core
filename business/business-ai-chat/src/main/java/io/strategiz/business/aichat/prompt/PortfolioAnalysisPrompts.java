package io.strategiz.business.aichat.prompt;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * System prompts for Portfolio AI Analysis feature.
 * Provides specialized prompts for Risk Analysis, Performance Analysis,
 * Rebalancing Suggestions, and Investment Opportunities.
 */
public class PortfolioAnalysisPrompts {

	public static final String SYSTEM_PROMPT = """
			You are a strategic portfolio analyst for Strategiz. Be decisive, data-driven, and actionable.

			Response Style:
			- Lead with your conclusion - no preamble
			- Be assertive: "Your portfolio shows..." not "It appears that..."
			- Use specific numbers and percentages
			- Provide one clear action when relevant
			- Skip disclaimers and filler words
			- 2-3 short paragraphs maximum

			Think like a strategist. Cut through noise. Deliver value.
			""";

	public static final String RISK_ANALYSIS_PROMPT = """
			Assess portfolio risk. Be direct.

			Format:
			**Risk Level: [HIGH/MEDIUM/LOW]** - One sentence why.

			**Primary Risk:** [Concentration/Volatility/Diversification] - Identify the biggest exposure with specific numbers.

			**Action:** One concrete step to reduce risk.

			Under 120 words. No hedging language.
			""";

	public static final String PERFORMANCE_ANALYSIS_PROMPT = """
			Analyze portfolio performance. Cut to the key metrics.

			Format:
			**Overall:** $X P&L (Y%) - One sentence verdict.

			**Winner:** [Symbol] +$X (+Y%) - Why it's outperforming.

			**Laggard:** [Symbol] -$X (-Y%) - Hold or cut?

			**Today:** +/-$X - What moved.

			Under 120 words. Be decisive on underperformers.
			""";

	public static final String REBALANCING_PROMPT = """
			Identify portfolio imbalances. Recommend specific moves.

			Format:
			**Current Split:** X% crypto / Y% stocks / Z% cash

			**Imbalance:** What's overweight or underweight with numbers.

			**Move:** "[Trim/Add] [Symbol] by $X to reach Y%" - One specific action.

			**Priority:** HIGH (>25% concentration) or MEDIUM

			Under 100 words. Give exact amounts, not ranges.
			""";

	public static final String OPPORTUNITIES_PROMPT = """
			Spot gaps and opportunities. One strategic move.

			Format:
			**Gap:** What's missing from this portfolio - be specific.

			**Opportunity:** One asset class or position to add, with rationale.

			**Move:** "Add $X to [Asset]" or "Allocate Y% to [Asset class]"

			Under 100 words. Focus on what would strengthen the portfolio strategically.
			""";

	/**
	 * Build contextual prompt by combining system prompt, specific insight type prompt,
	 * and actual portfolio data from the user's account.
	 *
	 * @param insightType Type of insight: "risk", "performance", "rebalancing", "opportunities"
	 * @param portfolioContext Portfolio data including totals, allocations, holdings, risk metrics
	 * @return Complete prompt with system instructions + context + specific focus
	 */
	public static String buildContextualPrompt(String insightType, Map<String, Object> portfolioContext) {
		StringBuilder prompt = new StringBuilder(SYSTEM_PROMPT);

		// Add portfolio context
		prompt.append("\n\n## Portfolio Context\n\n");
		prompt.append(formatPortfolioContext(portfolioContext));

		// Add specific insight prompt
		prompt.append("\n\n## Analysis Focus\n\n");
		String specificPrompt = switch (insightType != null ? insightType.toLowerCase() : "overview") {
			case "risk" -> RISK_ANALYSIS_PROMPT;
			case "performance" -> PERFORMANCE_ANALYSIS_PROMPT;
			case "rebalancing" -> REBALANCING_PROMPT;
			case "opportunities" -> OPPORTUNITIES_PROMPT;
			default -> "Provide a comprehensive overview of the portfolio covering risk, performance, and opportunities.";
		};
		prompt.append(specificPrompt);

		return prompt.toString();
	}

	/**
	 * Format portfolio context data into readable text for AI consumption
	 */
	@SuppressWarnings("unchecked")
	private static String formatPortfolioContext(Map<String, Object> portfolioContext) {
		if (portfolioContext == null || portfolioContext.isEmpty()) {
			return "No portfolio data available. User needs to connect brokerage/exchange accounts.";
		}

		StringBuilder context = new StringBuilder();

		// Check if user has portfolio
		Boolean hasPortfolio = (Boolean) portfolioContext.get("hasPortfolio");
		if (Boolean.FALSE.equals(hasPortfolio)) {
			Integer connectedProviders = (Integer) portfolioContext.getOrDefault("connectedProviders", 0);
			if (connectedProviders > 0) {
				return String.format("User has %d connected account(s) but no holdings data yet. Accounts may be syncing.", connectedProviders);
			}
			return "No portfolio data available. User should connect brokerage or exchange accounts to get insights.";
		}

		// Portfolio Totals
		context.append("**Portfolio Totals:**\n");
		context.append(String.format("- Total Value: $%s\n",
				formatNumber(portfolioContext.get("totalValue"))));
		context.append(String.format("- Day Change: $%s (%s%%)\n",
				formatNumber(portfolioContext.get("dayChange")),
				formatNumber(portfolioContext.get("dayChangePercent"))));
		context.append(String.format("- Total P&L: $%s (%s%%)\n",
				formatNumber(portfolioContext.get("totalProfitLoss")),
				formatNumber(portfolioContext.get("totalProfitLossPercent"))));
		context.append(String.format("- Cash Balance: $%s\n",
				formatNumber(portfolioContext.get("cashBalance"))));
		context.append(String.format("- Connected Accounts: %d\n",
				portfolioContext.getOrDefault("connectedProviders", 0)));
		context.append(String.format("- Total Positions: %d\n\n",
				portfolioContext.getOrDefault("totalPositions", 0)));

		// Asset Allocation
		Map<String, Object> allocation = (Map<String, Object>) portfolioContext.get("allocation");
		if (allocation != null && !allocation.isEmpty()) {
			context.append("**Asset Allocation:**\n");
			context.append(String.format("- Cryptocurrency: %s%%\n", formatNumber(allocation.get("cryptoPercent"))));
			context.append(String.format("- Stocks: %s%%\n", formatNumber(allocation.get("stockPercent"))));
			context.append(String.format("- Forex: %s%%\n", formatNumber(allocation.get("forexPercent"))));
			context.append(String.format("- Cash: %s%%\n", formatNumber(allocation.get("cashPercent"))));
			if (allocation.get("otherPercent") != null) {
				BigDecimal otherPercent = (BigDecimal) allocation.get("otherPercent");
				if (otherPercent.compareTo(BigDecimal.ZERO) > 0) {
					context.append(String.format("- Other: %s%%\n", formatNumber(otherPercent)));
				}
			}
			context.append("\n");
		}

		// Top Holdings
		List<Map<String, Object>> topHoldings = (List<Map<String, Object>>) portfolioContext.get("topHoldings");
		if (topHoldings != null && !topHoldings.isEmpty()) {
			context.append("**Top Holdings:**\n");
			for (int i = 0; i < topHoldings.size() && i < 5; i++) {
				Map<String, Object> holding = topHoldings.get(i);
				context.append(String.format("%d. %s (%s) - Value: $%s, P&L: $%s (%s%%), Type: %s\n",
						i + 1,
						holding.get("name"),
						holding.get("symbol"),
						formatNumber(holding.get("currentValue")),
						formatNumber(holding.get("profitLoss")),
						formatNumber(holding.get("profitLossPercent")),
						holding.get("assetType")));
			}
			context.append("\n");
		}

		// Risk Metrics
		Map<String, Object> riskMetrics = (Map<String, Object>) portfolioContext.get("riskMetrics");
		if (riskMetrics != null && !riskMetrics.isEmpty()) {
			context.append("**Risk Metrics:**\n");
			context.append(String.format("- Concentration Risk: %s\n", riskMetrics.get("concentrationRisk")));
			if (riskMetrics.get("largestPosition") != null) {
				context.append(String.format("- Largest Position: %s (%s%% of portfolio)\n",
						riskMetrics.get("largestPosition"),
						formatNumber(riskMetrics.get("largestPositionPercent"))));
			}
			context.append(String.format("- Diversification Score: %s (%d positions)\n",
					riskMetrics.get("diversificationScore"),
					riskMetrics.get("positionCount")));
		}

		return context.toString();
	}

	/**
	 * Format number for display in context (handles BigDecimal, Integer, etc.)
	 */
	private static String formatNumber(Object value) {
		if (value == null) {
			return "0.00";
		}
		if (value instanceof BigDecimal bigDecimal) {
			return bigDecimal.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
		}
		if (value instanceof Number number) {
			return String.format("%.2f", number.doubleValue());
		}
		return value.toString();
	}

}
