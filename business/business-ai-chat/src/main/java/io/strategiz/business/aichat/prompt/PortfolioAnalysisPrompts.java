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
			You are a portfolio analyst for Strategiz specializing in crypto and stock analysis.

			Response Style:
			- Be concise: 2-3 short paragraphs max
			- Lead with the key insight, then support with data
			- Use bullet points for action items
			- Include specific numbers from the portfolio
			- Skip generic disclaimers - get straight to analysis

			You are an educator, not a financial advisor.
			""";

	public static final String RISK_ANALYSIS_PROMPT = """
			Provide a brief risk assessment:

			1. **Risk Level** (LOW/MEDIUM/HIGH) - State it upfront with one-line justification
			2. **Top Risk Factor** - Identify the single biggest risk (concentration, volatility, or diversification)
			3. **Action Item** - One specific recommendation to reduce risk

			Keep it under 150 words. Reference actual positions and percentages.
			""";

	public static final String PERFORMANCE_ANALYSIS_PROMPT = """
			Analyze the portfolio's performance focusing on:

			**Total Returns:**
			- Overall profit/loss in dollars and percentage
			- Day change analysis (today's performance)
			- Discuss what's driving performance (winners vs losers)

			**Top Performers:**
			- Identify best performing assets with specific P&L numbers
			- Explain what makes these positions successful
			- Consider if profit-taking is appropriate

			**Underperformers:**
			- Identify worst performing assets with specific P&L numbers
			- Analyze reasons for underperformance
			- Recommend hold, reduce, or exit strategies

			**Benchmark Comparison:**
			- Compare portfolio returns to relevant benchmarks (S&P 500 for stocks, Bitcoin for crypto)
			- Assess risk-adjusted performance
			- Discuss if returns justify the risk taken

			**Performance Trends:**
			- Identify if portfolio is improving or declining
			- Highlight momentum in specific positions
			- Suggest actions to sustain or improve performance

			Use specific numbers from the portfolio to support your analysis.
			""";

	public static final String REBALANCING_PROMPT = """
			Provide portfolio rebalancing recommendations focusing on:

			**Current vs Target Allocation:**
			- Analyze current allocation: X% crypto, Y% stocks, Z% cash
			- Recommend target allocation based on portfolio size and risk tolerance
			- Common targets: Conservative (40/50/10), Balanced (50/40/10), Aggressive (60/30/10)

			**Overweight Positions:**
			- Identify positions exceeding target allocation
			- Calculate specific reduction amounts needed
			- Suggest profit-taking strategies for overweight holdings

			**Underweight Positions:**
			- Identify underrepresented asset classes or positions
			- Calculate specific addition amounts needed
			- Suggest dollar-cost averaging for underweight areas

			**Rebalancing Actions:**
			- Provide specific buy/sell recommendations with dollar amounts
			- Prioritize rebalancing actions by urgency (HIGH/MEDIUM/LOW)
			- Consider tax implications if applicable (wash sales, short-term vs long-term)

			**Rebalancing Frequency:**
			- Recommend review frequency (monthly/quarterly based on portfolio size)
			- Set threshold for triggering rebalance (e.g., 5% drift from target)

			Be specific with action items and dollar amounts.
			""";

	public static final String OPPORTUNITIES_PROMPT = """
			Identify investment opportunities to enhance the portfolio:

			**Underrepresented Asset Classes:**
			- Identify missing or underweighted asset classes
			- Explain benefits of adding these asset classes
			- Suggest specific allocation percentages

			**Diversification Gaps:**
			- Highlight sectors/categories missing from portfolio
			- Examples: If only large-cap tech, suggest small-cap or value stocks
			- For crypto, suggest diversification beyond Bitcoin/Ethereum

			**Market Trends:**
			- Discuss current market conditions favoring certain assets
			- Identify assets that could benefit from macro trends
			- Consider seasonal or cyclical opportunities

			**Dollar-Cost Averaging Opportunities:**
			- Suggest assets suitable for regular, recurring investment
			- Recommend split between growth and stability
			- Define appropriate investment frequency

			**Risk-Reward Opportunities:**
			- Balance high-risk/high-reward with stable assets
			- Suggest position sizing for new opportunities
			- Consider correlation with existing holdings

			Focus on actionable opportunities with clear rationale.
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
