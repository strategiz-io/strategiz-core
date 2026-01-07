package io.strategiz.client.execution.model;

import java.util.List;

/**
 * Response from batch strategy execution.
 */
public class ExecuteListResponse {

	private final boolean success;

	private final List<DeploymentResult> results;

	private final int totalDeployments;

	private final int successfulDeployments;

	private final int failedDeployments;

	private final int executionTimeMs;

	private final String error;

	private ExecuteListResponse(Builder builder) {
		this.success = builder.success;
		this.results = builder.results;
		this.totalDeployments = builder.totalDeployments;
		this.successfulDeployments = builder.successfulDeployments;
		this.failedDeployments = builder.failedDeployments;
		this.executionTimeMs = builder.executionTimeMs;
		this.error = builder.error;
	}

	public static Builder builder() {
		return new Builder();
	}

	public boolean isSuccess() {
		return success;
	}

	public List<DeploymentResult> getResults() {
		return results;
	}

	public int getTotalDeployments() {
		return totalDeployments;
	}

	public int getSuccessfulDeployments() {
		return successfulDeployments;
	}

	public int getFailedDeployments() {
		return failedDeployments;
	}

	public int getExecutionTimeMs() {
		return executionTimeMs;
	}

	public String getError() {
		return error;
	}

	/**
	 * Get all results with actionable signals (BUY or SELL).
	 */
	public List<DeploymentResult> getActionableResults() {
		if (results == null) {
			return List.of();
		}
		return results.stream().filter(DeploymentResult::hasActionableSignal).toList();
	}

	/**
	 * Get all alert results with actionable signals.
	 */
	public List<DeploymentResult> getActionableAlerts() {
		if (results == null) {
			return List.of();
		}
		return results.stream().filter(r -> r.isAlert() && r.hasActionableSignal()).toList();
	}

	/**
	 * Get all bot results with actionable signals.
	 */
	public List<DeploymentResult> getActionableBots() {
		if (results == null) {
			return List.of();
		}
		return results.stream().filter(r -> r.isBot() && r.hasActionableSignal()).toList();
	}

	public static class Builder {

		private boolean success;

		private List<DeploymentResult> results;

		private int totalDeployments;

		private int successfulDeployments;

		private int failedDeployments;

		private int executionTimeMs;

		private String error;

		public Builder success(boolean success) {
			this.success = success;
			return this;
		}

		public Builder results(List<DeploymentResult> results) {
			this.results = results;
			return this;
		}

		public Builder totalDeployments(int totalDeployments) {
			this.totalDeployments = totalDeployments;
			return this;
		}

		public Builder successfulDeployments(int successfulDeployments) {
			this.successfulDeployments = successfulDeployments;
			return this;
		}

		public Builder failedDeployments(int failedDeployments) {
			this.failedDeployments = failedDeployments;
			return this;
		}

		public Builder executionTimeMs(int executionTimeMs) {
			this.executionTimeMs = executionTimeMs;
			return this;
		}

		public Builder error(String error) {
			this.error = error;
			return this;
		}

		public ExecuteListResponse build() {
			return new ExecuteListResponse(this);
		}

	}

}
