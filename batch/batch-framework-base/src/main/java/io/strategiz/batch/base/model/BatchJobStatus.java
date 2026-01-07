package io.strategiz.batch.base.model;

import java.time.Duration;
import java.time.Instant;

/**
 * Status snapshot of a batch job including current state and last execution info.
 */
public class BatchJobStatus {

	private final String jobId;

	private final String jobName;

	private final boolean enabled;

	private final boolean running;

	private final Instant lastExecutionStart;

	private final Instant lastExecutionEnd;

	private final BatchJobResult lastResult;

	private BatchJobStatus(Builder builder) {
		this.jobId = builder.jobId;
		this.jobName = builder.jobName;
		this.enabled = builder.enabled;
		this.running = builder.running;
		this.lastExecutionStart = builder.lastExecutionStart;
		this.lastExecutionEnd = builder.lastExecutionEnd;
		this.lastResult = builder.lastResult;
	}

	public String getJobId() {
		return jobId;
	}

	public String getJobName() {
		return jobName;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public boolean isRunning() {
		return running;
	}

	public Instant getLastExecutionStart() {
		return lastExecutionStart;
	}

	public Instant getLastExecutionEnd() {
		return lastExecutionEnd;
	}

	public BatchJobResult getLastResult() {
		return lastResult;
	}

	public Duration getLastExecutionDuration() {
		if (lastExecutionStart != null && lastExecutionEnd != null) {
			return Duration.between(lastExecutionStart, lastExecutionEnd);
		}
		return null;
	}

	public boolean hasEverRun() {
		return lastExecutionStart != null;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private String jobId;

		private String jobName;

		private boolean enabled;

		private boolean running;

		private Instant lastExecutionStart;

		private Instant lastExecutionEnd;

		private BatchJobResult lastResult;

		public Builder jobId(String jobId) {
			this.jobId = jobId;
			return this;
		}

		public Builder jobName(String jobName) {
			this.jobName = jobName;
			return this;
		}

		public Builder enabled(boolean enabled) {
			this.enabled = enabled;
			return this;
		}

		public Builder running(boolean running) {
			this.running = running;
			return this;
		}

		public Builder lastExecutionStart(Instant lastExecutionStart) {
			this.lastExecutionStart = lastExecutionStart;
			return this;
		}

		public Builder lastExecutionEnd(Instant lastExecutionEnd) {
			this.lastExecutionEnd = lastExecutionEnd;
			return this;
		}

		public Builder lastResult(BatchJobResult lastResult) {
			this.lastResult = lastResult;
			return this;
		}

		public BatchJobStatus build() {
			return new BatchJobStatus(this);
		}

	}

}
