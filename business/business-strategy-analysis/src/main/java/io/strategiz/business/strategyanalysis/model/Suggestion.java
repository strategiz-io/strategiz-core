package io.strategiz.business.strategyanalysis.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A single actionable suggestion for improving a strategy.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Suggestion {

	@JsonProperty("issue")
	private String issue; // Brief description of the problem

	@JsonProperty("recommendation")
	private String recommendation; // Specific fix to apply

	@JsonProperty("example")
	private String example; // Code example showing the fix

	@JsonProperty("priority")
	private String priority; // "high", "medium", "low"

	@JsonProperty("expectedImpact")
	private ExpectedImpact expectedImpact; // Optional: projected improvement

	public Suggestion() {
	}

	public Suggestion(String issue, String recommendation, String example, String priority) {
		this.issue = issue;
		this.recommendation = recommendation;
		this.example = example;
		this.priority = priority;
	}

	// Getters and setters

	public String getIssue() {
		return issue;
	}

	public void setIssue(String issue) {
		this.issue = issue;
	}

	public String getRecommendation() {
		return recommendation;
	}

	public void setRecommendation(String recommendation) {
		this.recommendation = recommendation;
	}

	public String getExample() {
		return example;
	}

	public void setExample(String example) {
		this.example = example;
	}

	public String getPriority() {
		return priority;
	}

	public void setPriority(String priority) {
		this.priority = priority;
	}

	public ExpectedImpact getExpectedImpact() {
		return expectedImpact;
	}

	public void setExpectedImpact(ExpectedImpact expectedImpact) {
		this.expectedImpact = expectedImpact;
	}

	/**
	 * Expected impact of applying this suggestion.
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class ExpectedImpact {

		@JsonProperty("metric")
		private String metric; // "sharpeRatio", "maxDrawdown", "winRate", etc.

		@JsonProperty("currentValue")
		private Double currentValue;

		@JsonProperty("expectedValue")
		private Double expectedValue;

		public ExpectedImpact() {
		}

		public ExpectedImpact(String metric, Double currentValue, Double expectedValue) {
			this.metric = metric;
			this.currentValue = currentValue;
			this.expectedValue = expectedValue;
		}

		// Getters and setters

		public String getMetric() {
			return metric;
		}

		public void setMetric(String metric) {
			this.metric = metric;
		}

		public Double getCurrentValue() {
			return currentValue;
		}

		public void setCurrentValue(Double currentValue) {
			this.currentValue = currentValue;
		}

		public Double getExpectedValue() {
			return expectedValue;
		}

		public void setExpectedValue(Double expectedValue) {
			this.expectedValue = expectedValue;
		}

	}

}
