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

	@JsonProperty("alphaModeUsed")
	private Boolean alphaModeUsed; // Whether Alpha Mode was used for this generation

	@JsonProperty("historicalInsights")
	private SymbolInsights historicalInsights; // Insights from Alpha Mode analysis

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

	public Boolean getAlphaModeUsed() {
		return alphaModeUsed;
	}

	public void setAlphaModeUsed(Boolean alphaModeUsed) {
		this.alphaModeUsed = alphaModeUsed;
	}

	public SymbolInsights getHistoricalInsights() {
		return historicalInsights;
	}

	public void setHistoricalInsights(SymbolInsights historicalInsights) {
		this.historicalInsights = historicalInsights;
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
