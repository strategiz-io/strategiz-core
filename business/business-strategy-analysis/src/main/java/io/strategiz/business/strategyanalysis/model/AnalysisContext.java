package io.strategiz.business.strategyanalysis.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * Context for strategy analysis, includes metadata and optional backtest results.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnalysisContext {

	@JsonProperty("symbol")
	private String symbol;

	@JsonProperty("timeframe")
	private String timeframe;

	@JsonProperty("period")
	private String period;

	@JsonProperty("strategyId")
	private String strategyId; // Optional: if analyzing existing strategy

	@JsonProperty("marketContext")
	private MarketContext marketContext; // Populated by StrategyAnalysisBusiness

	@JsonProperty("diagnostic")
	private StrategyDiagnostic diagnostic; // Populated by StrategyAnalysisBusiness

	@JsonProperty("backtestResults")
	private Map<String, Object> backtestResults; // Optional: null for NO_SIGNALS mode

	public AnalysisContext() {
	}

	// Getters and setters

	public String getSymbol() {
		return symbol;
	}

	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}

	public String getTimeframe() {
		return timeframe;
	}

	public void setTimeframe(String timeframe) {
		this.timeframe = timeframe;
	}

	public String getPeriod() {
		return period;
	}

	public void setPeriod(String period) {
		this.period = period;
	}

	public String getStrategyId() {
		return strategyId;
	}

	public void setStrategyId(String strategyId) {
		this.strategyId = strategyId;
	}

	public MarketContext getMarketContext() {
		return marketContext;
	}

	public void setMarketContext(MarketContext marketContext) {
		this.marketContext = marketContext;
	}

	public StrategyDiagnostic getDiagnostic() {
		return diagnostic;
	}

	public void setDiagnostic(StrategyDiagnostic diagnostic) {
		this.diagnostic = diagnostic;
	}

	public Map<String, Object> getBacktestResults() {
		return backtestResults;
	}

	public void setBacktestResults(Map<String, Object> backtestResults) {
		this.backtestResults = backtestResults;
	}

}
