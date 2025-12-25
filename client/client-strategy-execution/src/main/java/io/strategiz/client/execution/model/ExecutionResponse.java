package io.strategiz.client.execution.model;

import java.util.List;

/**
 * Response from strategy execution
 */
public class ExecutionResponse {

	private boolean success;

	private String error;

	private List<Signal> signals;

	private List<Indicator> indicators;

	private Performance performance;

	private long executionTimeMs;

	private List<String> logs;

	public ExecutionResponse() {
	}

	public ExecutionResponse(boolean success, String error, List<Signal> signals, List<Indicator> indicators,
			Performance performance, long executionTimeMs, List<String> logs) {
		this.success = success;
		this.error = error;
		this.signals = signals;
		this.indicators = indicators;
		this.performance = performance;
		this.executionTimeMs = executionTimeMs;
		this.logs = logs;
	}

	public static ExecutionResponseBuilder builder() {
		return new ExecutionResponseBuilder();
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

	public List<Signal> getSignals() {
		return signals;
	}

	public void setSignals(List<Signal> signals) {
		this.signals = signals;
	}

	public List<Indicator> getIndicators() {
		return indicators;
	}

	public void setIndicators(List<Indicator> indicators) {
		this.indicators = indicators;
	}

	public Performance getPerformance() {
		return performance;
	}

	public void setPerformance(Performance performance) {
		this.performance = performance;
	}

	public long getExecutionTimeMs() {
		return executionTimeMs;
	}

	public void setExecutionTimeMs(long executionTimeMs) {
		this.executionTimeMs = executionTimeMs;
	}

	public List<String> getLogs() {
		return logs;
	}

	public void setLogs(List<String> logs) {
		this.logs = logs;
	}

	public static class ExecutionResponseBuilder {

		private boolean success;

		private String error;

		private List<Signal> signals;

		private List<Indicator> indicators;

		private Performance performance;

		private long executionTimeMs;

		private List<String> logs;

		public ExecutionResponseBuilder success(boolean success) {
			this.success = success;
			return this;
		}

		public ExecutionResponseBuilder error(String error) {
			this.error = error;
			return this;
		}

		public ExecutionResponseBuilder signals(List<Signal> signals) {
			this.signals = signals;
			return this;
		}

		public ExecutionResponseBuilder indicators(List<Indicator> indicators) {
			this.indicators = indicators;
			return this;
		}

		public ExecutionResponseBuilder performance(Performance performance) {
			this.performance = performance;
			return this;
		}

		public ExecutionResponseBuilder executionTimeMs(long executionTimeMs) {
			this.executionTimeMs = executionTimeMs;
			return this;
		}

		public ExecutionResponseBuilder logs(List<String> logs) {
			this.logs = logs;
			return this;
		}

		public ExecutionResponse build() {
			return new ExecutionResponse(success, error, signals, indicators, performance, executionTimeMs, logs);
		}

	}

}
