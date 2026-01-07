package io.strategiz.client.execution.model;

/**
 * Result of executing a single deployment (alert or bot).
 */
public class DeploymentResult {

	private final String deploymentId;

	private final String deploymentType;

	private final boolean success;

	private final LiveSignal signal;

	private final int executionTimeMs;

	private final String error;

	private DeploymentResult(Builder builder) {
		this.deploymentId = builder.deploymentId;
		this.deploymentType = builder.deploymentType;
		this.success = builder.success;
		this.signal = builder.signal;
		this.executionTimeMs = builder.executionTimeMs;
		this.error = builder.error;
	}

	public static Builder builder() {
		return new Builder();
	}

	public String getDeploymentId() {
		return deploymentId;
	}

	public String getDeploymentType() {
		return deploymentType;
	}

	public boolean isSuccess() {
		return success;
	}

	public LiveSignal getSignal() {
		return signal;
	}

	public int getExecutionTimeMs() {
		return executionTimeMs;
	}

	public String getError() {
		return error;
	}

	public boolean isAlert() {
		return "ALERT".equals(deploymentType);
	}

	public boolean isBot() {
		return "BOT".equals(deploymentType);
	}

	public boolean hasActionableSignal() {
		return signal != null && signal.isActionable();
	}

	public static class Builder {

		private String deploymentId;

		private String deploymentType;

		private boolean success;

		private LiveSignal signal;

		private int executionTimeMs;

		private String error;

		public Builder deploymentId(String deploymentId) {
			this.deploymentId = deploymentId;
			return this;
		}

		public Builder deploymentType(String deploymentType) {
			this.deploymentType = deploymentType;
			return this;
		}

		public Builder success(boolean success) {
			this.success = success;
			return this;
		}

		public Builder signal(LiveSignal signal) {
			this.signal = signal;
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

		public DeploymentResult build() {
			return new DeploymentResult(this);
		}

	}

}
