package io.strategiz.service.labs.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.strategiz.business.historicalinsights.model.SymbolInsights;

import java.util.List;
import java.util.Map;

/**
 * Response DTO for AI-powered strategy generation.
 */
public class AIStrategyResponse {

	/**
	 * Risk level classification for strategies.
	 */
	public enum RiskLevel {

		LOW, MEDIUM, HIGH, AGGRESSIVE

	}

	@JsonProperty("visualConfig")
	private Map<String, Object> visualConfig;

	@JsonProperty("pythonCode")
	private String pythonCode;

	@JsonProperty("pineScriptCode")
	private String pineScriptCode;

	@JsonProperty("summaryCard")
	private String summaryCard;

	@JsonProperty("riskLevel")
	private RiskLevel riskLevel;

	@JsonProperty("detectedIndicators")
	private List<String> detectedIndicators;

	@JsonProperty("explanation")
	private String explanation;

	@JsonProperty("suggestions")
	private List<String> suggestions;

	@JsonProperty("optimizationSuggestions")
	private List<OptimizationSuggestion> optimizationSuggestions;

	@JsonProperty("canRepresentVisually")
	private boolean canRepresentVisually = true;

	@JsonProperty("warning")
	private String warning;

	@JsonProperty("success")
	private boolean success = true;

	@JsonProperty("error")
	private String error;

	@JsonProperty("historicalInsightsUsed")
	private Boolean historicalInsightsUsed; // Whether Historical Market Insights was used for this generation

	@JsonProperty("historicalInsights")
	private SymbolInsights historicalInsights; // Historical market data insights (7 years analyzed)

	@JsonProperty("optimizationSummary")
	private OptimizationSummary optimizationSummary; // Summary of optimization changes and improvements

	@JsonProperty("autonomousModeUsed")
	private String autonomousModeUsed; // Which autonomous mode was used: "GENERATIVE_AI" or "AUTONOMOUS"

	// Getters and Setters

	public Map<String, Object> getVisualConfig() {
		return visualConfig;
	}

	public void setVisualConfig(Map<String, Object> visualConfig) {
		this.visualConfig = visualConfig;
	}

	public String getPythonCode() {
		return pythonCode;
	}

	public void setPythonCode(String pythonCode) {
		this.pythonCode = pythonCode;
	}

	public String getPineScriptCode() {
		return pineScriptCode;
	}

	public void setPineScriptCode(String pineScriptCode) {
		this.pineScriptCode = pineScriptCode;
	}

	public String getSummaryCard() {
		return summaryCard;
	}

	public void setSummaryCard(String summaryCard) {
		this.summaryCard = summaryCard;
	}

	public RiskLevel getRiskLevel() {
		return riskLevel;
	}

	public void setRiskLevel(RiskLevel riskLevel) {
		this.riskLevel = riskLevel;
	}

	public List<String> getDetectedIndicators() {
		return detectedIndicators;
	}

	public void setDetectedIndicators(List<String> detectedIndicators) {
		this.detectedIndicators = detectedIndicators;
	}

	public String getExplanation() {
		return explanation;
	}

	public void setExplanation(String explanation) {
		this.explanation = explanation;
	}

	public List<String> getSuggestions() {
		return suggestions;
	}

	public void setSuggestions(List<String> suggestions) {
		this.suggestions = suggestions;
	}

	public List<OptimizationSuggestion> getOptimizationSuggestions() {
		return optimizationSuggestions;
	}

	public void setOptimizationSuggestions(List<OptimizationSuggestion> optimizationSuggestions) {
		this.optimizationSuggestions = optimizationSuggestions;
	}

	public boolean isCanRepresentVisually() {
		return canRepresentVisually;
	}

	public void setCanRepresentVisually(boolean canRepresentVisually) {
		this.canRepresentVisually = canRepresentVisually;
	}

	public String getWarning() {
		return warning;
	}

	public void setWarning(String warning) {
		this.warning = warning;
	}

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public String getError() {
		return error;
	}

	public void setError(String error) {
		this.error = error;
	}

	public Boolean getHistoricalInsightsUsed() {
		return historicalInsightsUsed;
	}

	public void setHistoricalInsightsUsed(Boolean historicalInsightsUsed) {
		this.historicalInsightsUsed = historicalInsightsUsed;
	}

	public SymbolInsights getHistoricalInsights() {
		return historicalInsights;
	}

	public void setHistoricalInsights(SymbolInsights historicalInsights) {
		this.historicalInsights = historicalInsights;
	}

	public OptimizationSummary getOptimizationSummary() {
		return optimizationSummary;
	}

	public void setOptimizationSummary(OptimizationSummary optimizationSummary) {
		this.optimizationSummary = optimizationSummary;
	}

	public String getAutonomousModeUsed() {
		return autonomousModeUsed;
	}

	public void setAutonomousModeUsed(String autonomousModeUsed) {
		this.autonomousModeUsed = autonomousModeUsed;
	}

	/**
	 * An optimization suggestion with expected impact.
	 */
	public static class OptimizationSuggestion {

		@JsonProperty("id")
		private String id;

		@JsonProperty("priority")
		private String priority; // "high", "medium", "low"

		@JsonProperty("description")
		private String description;

		@JsonProperty("rationale")
		private String rationale;

		@JsonProperty("impact")
		private Impact impact;

		@JsonProperty("patch")
		private Patch patch;

		// Getters and Setters

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getPriority() {
			return priority;
		}

		public void setPriority(String priority) {
			this.priority = priority;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		public String getRationale() {
			return rationale;
		}

		public void setRationale(String rationale) {
			this.rationale = rationale;
		}

		public Impact getImpact() {
			return impact;
		}

		public void setImpact(Impact impact) {
			this.impact = impact;
		}

		public Patch getPatch() {
			return patch;
		}

		public void setPatch(Patch patch) {
			this.patch = patch;
		}

	}

	/**
	 * Expected impact of an optimization.
	 */
	public static class Impact {

		@JsonProperty("metric")
		private String metric;

		@JsonProperty("expectedBefore")
		private double expectedBefore;

		@JsonProperty("expectedAfter")
		private double expectedAfter;

		// Getters and Setters

		public String getMetric() {
			return metric;
		}

		public void setMetric(String metric) {
			this.metric = metric;
		}

		public double getExpectedBefore() {
			return expectedBefore;
		}

		public void setExpectedBefore(double expectedBefore) {
			this.expectedBefore = expectedBefore;
		}

		public double getExpectedAfter() {
			return expectedAfter;
		}

		public void setExpectedAfter(double expectedAfter) {
			this.expectedAfter = expectedAfter;
		}

	}

	/**
	 * A patch to apply an optimization.
	 */
	public static class Patch {

		@JsonProperty("visualConfig")
		private Map<String, Object> visualConfig;

		@JsonProperty("codeSnippet")
		private String codeSnippet;

		// Getters and Setters

		public Map<String, Object> getVisualConfig() {
			return visualConfig;
		}

		public void setVisualConfig(Map<String, Object> visualConfig) {
			this.visualConfig = visualConfig;
		}

		public String getCodeSnippet() {
			return codeSnippet;
		}

		public void setCodeSnippet(String codeSnippet) {
			this.codeSnippet = codeSnippet;
		}

	}

	/**
	 * Summary of strategy optimization with baseline comparison and change tracking.
	 */
	public static class OptimizationSummary {

		@JsonProperty("mode")
		private String mode; // "GENERATE_NEW" or "ENHANCE_EXISTING"

		@JsonProperty("baselineMetrics")
		private Map<String, Double> baselineMetrics;

		@JsonProperty("optimizedMetrics")
		private Map<String, Double> optimizedMetrics;

		@JsonProperty("changes")
		private List<StrategyChange> changes;

		@JsonProperty("improvementRationale")
		private String improvementRationale;

		// Getters and Setters

		public String getMode() {
			return mode;
		}

		public void setMode(String mode) {
			this.mode = mode;
		}

		public Map<String, Double> getBaselineMetrics() {
			return baselineMetrics;
		}

		public void setBaselineMetrics(Map<String, Double> baselineMetrics) {
			this.baselineMetrics = baselineMetrics;
		}

		public Map<String, Double> getOptimizedMetrics() {
			return optimizedMetrics;
		}

		public void setOptimizedMetrics(Map<String, Double> optimizedMetrics) {
			this.optimizedMetrics = optimizedMetrics;
		}

		public List<StrategyChange> getChanges() {
			return changes;
		}

		public void setChanges(List<StrategyChange> changes) {
			this.changes = changes;
		}

		public String getImprovementRationale() {
			return improvementRationale;
		}

		public void setImprovementRationale(String improvementRationale) {
			this.improvementRationale = improvementRationale;
		}

	}

	/**
	 * Represents a specific change made during strategy optimization.
	 */
	public static class StrategyChange {

		@JsonProperty("category")
		private String category; // "PARAMETER", "LOGIC", "INDICATOR", "RISK_MANAGEMENT"

		@JsonProperty("field")
		private String field;

		@JsonProperty("oldValue")
		private String oldValue;

		@JsonProperty("newValue")
		private String newValue;

		@JsonProperty("rationale")
		private String rationale;

		// Getters and Setters

		public String getCategory() {
			return category;
		}

		public void setCategory(String category) {
			this.category = category;
		}

		public String getField() {
			return field;
		}

		public void setField(String field) {
			this.field = field;
		}

		public String getOldValue() {
			return oldValue;
		}

		public void setOldValue(String oldValue) {
			this.oldValue = oldValue;
		}

		public String getNewValue() {
			return newValue;
		}

		public void setNewValue(String newValue) {
			this.newValue = newValue;
		}

		public String getRationale() {
			return rationale;
		}

		public void setRationale(String rationale) {
			this.rationale = rationale;
		}

	}

	// Builder pattern for convenience

	public static AIStrategyResponse success(Map<String, Object> visualConfig, String pythonCode) {
		AIStrategyResponse response = new AIStrategyResponse();
		response.setSuccess(true);
		response.setVisualConfig(visualConfig);
		response.setPythonCode(pythonCode);
		return response;
	}

	public static AIStrategyResponse error(String errorMessage) {
		AIStrategyResponse response = new AIStrategyResponse();
		response.setSuccess(false);
		response.setError(errorMessage);
		return response;
	}

}
