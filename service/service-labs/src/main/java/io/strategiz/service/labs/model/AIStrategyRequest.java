package io.strategiz.service.labs.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.Map;

/**
 * Request DTO for AI-powered strategy generation, explanation, and optimization.
 */
public class AIStrategyRequest {

	/**
	 * The type of AI request being made.
	 */
	public enum RequestType {

		GENERATE, // Generate a new strategy from prompt
		REFINE, // Refine an existing strategy
		EXPLAIN, // Explain a specific element
		OPTIMIZE, // Get optimization suggestions from backtest
		PARSE_CODE, // Parse code to visual config
		BACKTEST_QUERY, // Parse natural language backtest query
		PREVIEW_INDICATORS // Detect indicators from partial prompt

	}

	/**
	 * Mode for strategy optimization.
	 */
	public enum OptimizationMode {

		GENERATE_NEW, // Create brand new strategy that beats baseline
		ENHANCE_EXISTING // Improve current strategy parameters/logic

	}

	@NotBlank(message = "Prompt is required")
	@JsonProperty("prompt")
	private String prompt;

	@JsonProperty("requestType")
	private RequestType requestType = RequestType.GENERATE;

	@JsonProperty("context")
	private StrategyContext context;

	@JsonProperty("conversationHistory")
	private List<ChatMessage> conversationHistory;

	@JsonProperty("backtestResults")
	private BacktestResults backtestResults;

	@JsonProperty("elementToExplain")
	private String elementToExplain;

	@JsonProperty("model")
	private String model; // LLM model to use (e.g., "gemini-1.5-flash", "claude-3-5-sonnet")

	@JsonProperty("visualEditorSchema")
	private String visualEditorSchema; // Schema description for generating valid visual rules

	@JsonProperty("useHistoricalInsights")
	private Boolean useHistoricalInsights = false; // Enable Historical Market Insights with 7 years of data analysis

	@JsonProperty("historicalInsightsOptions")
	private HistoricalMarketInsightsOptions historicalInsightsOptions; // Options for Historical Market Insights analysis

	@JsonProperty("optimizationMode")
	private OptimizationMode optimizationMode = OptimizationMode.ENHANCE_EXISTING; // Mode for strategy optimization

	// Getters and Setters

	public String getPrompt() {
		return prompt;
	}

	public void setPrompt(String prompt) {
		this.prompt = prompt;
	}

	public RequestType getRequestType() {
		return requestType;
	}

	public void setRequestType(RequestType requestType) {
		this.requestType = requestType;
	}

	public StrategyContext getContext() {
		return context;
	}

	public void setContext(StrategyContext context) {
		this.context = context;
	}

	public List<ChatMessage> getConversationHistory() {
		return conversationHistory;
	}

	public void setConversationHistory(List<ChatMessage> conversationHistory) {
		this.conversationHistory = conversationHistory;
	}

	public BacktestResults getBacktestResults() {
		return backtestResults;
	}

	public void setBacktestResults(BacktestResults backtestResults) {
		this.backtestResults = backtestResults;
	}

	public String getElementToExplain() {
		return elementToExplain;
	}

	public void setElementToExplain(String elementToExplain) {
		this.elementToExplain = elementToExplain;
	}

	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	public String getVisualEditorSchema() {
		return visualEditorSchema;
	}

	public void setVisualEditorSchema(String visualEditorSchema) {
		this.visualEditorSchema = visualEditorSchema;
	}

	public Boolean getUseHistoricalInsights() {
		return useHistoricalInsights;
	}

	public void setUseHistoricalInsights(Boolean useHistoricalInsights) {
		this.useHistoricalInsights = useHistoricalInsights;
	}

	public HistoricalMarketInsightsOptions getHistoricalInsightsOptions() {
		return historicalInsightsOptions;
	}

	public void setHistoricalInsightsOptions(HistoricalMarketInsightsOptions historicalInsightsOptions) {
		this.historicalInsightsOptions = historicalInsightsOptions;
	}

	public OptimizationMode getOptimizationMode() {
		return optimizationMode;
	}

	public void setOptimizationMode(OptimizationMode optimizationMode) {
		this.optimizationMode = optimizationMode;
	}

	/**
	 * Options for Historical Market Insights analysis (Feeling Lucky mode).
	 * Analyzes 7 years of historical market data to generate optimized strategies.
	 */
	public static class HistoricalMarketInsightsOptions {

		@JsonProperty("lookbackDays")
		private Integer lookbackDays = 2600; // ~7 years default

		@JsonProperty("useFundamentals")
		private Boolean useFundamentals = false; // Include fundamental analysis

		@JsonProperty("forceRefresh")
		private Boolean forceRefresh = false; // Skip cache and recompute

		// Getters and Setters

		public Integer getLookbackDays() {
			return lookbackDays;
		}

		public void setLookbackDays(Integer lookbackDays) {
			this.lookbackDays = lookbackDays;
		}

		public Boolean getUseFundamentals() {
			return useFundamentals;
		}

		public void setUseFundamentals(Boolean useFundamentals) {
			this.useFundamentals = useFundamentals;
		}

		public Boolean getForceRefresh() {
			return forceRefresh;
		}

		public void setForceRefresh(Boolean forceRefresh) {
			this.forceRefresh = forceRefresh;
		}

	}

	/**
	 * Context about the current strategy state.
	 */
	public static class StrategyContext {

		@JsonProperty("currentVisualConfig")
		private Map<String, Object> currentVisualConfig;

		@JsonProperty("currentCode")
		private String currentCode;

		@JsonProperty("symbols")
		private List<String> symbols;

		@JsonProperty("timeframe")
		private String timeframe;

		@JsonProperty("strategyId")
		private String strategyId;

		// Getters and Setters

		public Map<String, Object> getCurrentVisualConfig() {
			return currentVisualConfig;
		}

		public void setCurrentVisualConfig(Map<String, Object> currentVisualConfig) {
			this.currentVisualConfig = currentVisualConfig;
		}

		public String getCurrentCode() {
			return currentCode;
		}

		public void setCurrentCode(String currentCode) {
			this.currentCode = currentCode;
		}

		public List<String> getSymbols() {
			return symbols;
		}

		public void setSymbols(List<String> symbols) {
			this.symbols = symbols;
		}

		public String getTimeframe() {
			return timeframe;
		}

		public void setTimeframe(String timeframe) {
			this.timeframe = timeframe;
		}

		public String getStrategyId() {
			return strategyId;
		}

		public void setStrategyId(String strategyId) {
			this.strategyId = strategyId;
		}

	}

	/**
	 * A message in the conversation history.
	 */
	public static class ChatMessage {

		@JsonProperty("role")
		private String role; // "user" or "assistant"

		@JsonProperty("content")
		private String content;

		@JsonProperty("timestamp")
		private String timestamp;

		// Getters and Setters

		public String getRole() {
			return role;
		}

		public void setRole(String role) {
			this.role = role;
		}

		public String getContent() {
			return content;
		}

		public void setContent(String content) {
			this.content = content;
		}

		public String getTimestamp() {
			return timestamp;
		}

		public void setTimestamp(String timestamp) {
			this.timestamp = timestamp;
		}

	}

	/**
	 * Backtest results for optimization requests.
	 */
	public static class BacktestResults {

		@JsonProperty("totalReturn")
		private double totalReturn;

		@JsonProperty("totalPnL")
		private double totalPnL;

		@JsonProperty("winRate")
		private double winRate;

		@JsonProperty("totalTrades")
		private int totalTrades;

		@JsonProperty("profitableTrades")
		private int profitableTrades;

		@JsonProperty("avgWin")
		private double avgWin;

		@JsonProperty("avgLoss")
		private double avgLoss;

		@JsonProperty("profitFactor")
		private double profitFactor;

		@JsonProperty("maxDrawdown")
		private double maxDrawdown;

		@JsonProperty("sharpeRatio")
		private double sharpeRatio;

		// Getters and Setters

		public double getTotalReturn() {
			return totalReturn;
		}

		public void setTotalReturn(double totalReturn) {
			this.totalReturn = totalReturn;
		}

		public double getTotalPnL() {
			return totalPnL;
		}

		public void setTotalPnL(double totalPnL) {
			this.totalPnL = totalPnL;
		}

		public double getWinRate() {
			return winRate;
		}

		public void setWinRate(double winRate) {
			this.winRate = winRate;
		}

		public int getTotalTrades() {
			return totalTrades;
		}

		public void setTotalTrades(int totalTrades) {
			this.totalTrades = totalTrades;
		}

		public int getProfitableTrades() {
			return profitableTrades;
		}

		public void setProfitableTrades(int profitableTrades) {
			this.profitableTrades = profitableTrades;
		}

		public double getAvgWin() {
			return avgWin;
		}

		public void setAvgWin(double avgWin) {
			this.avgWin = avgWin;
		}

		public double getAvgLoss() {
			return avgLoss;
		}

		public void setAvgLoss(double avgLoss) {
			this.avgLoss = avgLoss;
		}

		public double getProfitFactor() {
			return profitFactor;
		}

		public void setProfitFactor(double profitFactor) {
			this.profitFactor = profitFactor;
		}

		public double getMaxDrawdown() {
			return maxDrawdown;
		}

		public void setMaxDrawdown(double maxDrawdown) {
			this.maxDrawdown = maxDrawdown;
		}

		public double getSharpeRatio() {
			return sharpeRatio;
		}

		public void setSharpeRatio(double sharpeRatio) {
			this.sharpeRatio = sharpeRatio;
		}

	}

}
