package io.strategiz.client.recaptcha.model;

import java.util.List;

/**
 * Result of a reCAPTCHA Enterprise assessment.
 *
 * Contains the risk score and classification reasons from Google's analysis.
 */
public class RiskAnalysis {

	private final double score;

	private final List<String> reasons;

	private final String tokenAction;

	private final boolean valid;

	private final String assessmentName;

	private RiskAnalysis(Builder builder) {
		this.score = builder.score;
		this.reasons = builder.reasons;
		this.tokenAction = builder.tokenAction;
		this.valid = builder.valid;
		this.assessmentName = builder.assessmentName;
	}

	/**
	 * Get the risk score. Range: 0.0 (likely bot) to 1.0 (likely human).
	 */
	public double getScore() {
		return score;
	}

	/**
	 * Get the classification reasons from the assessment.
	 */
	public List<String> getReasons() {
		return reasons;
	}

	/**
	 * Get the action associated with the token.
	 */
	public String getTokenAction() {
		return tokenAction;
	}

	/**
	 * Check if the token was valid.
	 */
	public boolean isValid() {
		return valid;
	}

	/**
	 * Get the full assessment name for annotation purposes.
	 */
	public String getAssessmentName() {
		return assessmentName;
	}

	/**
	 * Check if the assessment indicates a likely human.
	 * @param threshold minimum score to consider human (typically 0.5)
	 */
	public boolean isLikelyHuman(double threshold) {
		return valid && score >= threshold;
	}

	/**
	 * Check if the assessment indicates a likely bot.
	 * @param threshold minimum score to consider human (typically 0.5)
	 */
	public boolean isLikelyBot(double threshold) {
		return !valid || score < threshold;
	}

	/**
	 * Create a mock result for development/testing.
	 */
	public static RiskAnalysis mock(double score) {
		return builder().score(score).valid(true).tokenAction("mock").assessmentName("mock-assessment").build();
	}

	/**
	 * Create a failed result when assessment fails.
	 */
	public static RiskAnalysis failed(String reason) {
		return builder().score(0.0).valid(false).reasons(List.of(reason)).build();
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private double score = 0.0;

		private List<String> reasons = List.of();

		private String tokenAction;

		private boolean valid = false;

		private String assessmentName;

		public Builder score(double score) {
			this.score = score;
			return this;
		}

		public Builder reasons(List<String> reasons) {
			this.reasons = reasons;
			return this;
		}

		public Builder tokenAction(String tokenAction) {
			this.tokenAction = tokenAction;
			return this;
		}

		public Builder valid(boolean valid) {
			this.valid = valid;
			return this;
		}

		public Builder assessmentName(String assessmentName) {
			this.assessmentName = assessmentName;
			return this;
		}

		public RiskAnalysis build() {
			return new RiskAnalysis(this);
		}

	}

	@Override
	public String toString() {
		return "RiskAnalysis{" + "score=" + score + ", valid=" + valid + ", reasons=" + reasons + ", tokenAction='"
				+ tokenAction + '\'' + '}';
	}

}
