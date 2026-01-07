package io.strategiz.batch.base.model;

import java.time.Duration;
import java.time.Instant;

/**
 * Base class for batch job results.
 *
 * Provides common result fields that all batch jobs should report.
 * Subclasses add job-specific fields.
 */
public class BatchJobResult {

	private final boolean success;

	private final String jobId;

	private final Instant startTime;

	private final Instant endTime;

	private final Duration duration;

	private final int itemsProcessed;

	private final int itemsSucceeded;

	private final int itemsFailed;

	private final String errorMessage;

	private final int retryAttempt;

	protected BatchJobResult(Builder<?> builder) {
		this.success = builder.success;
		this.jobId = builder.jobId;
		this.startTime = builder.startTime;
		this.endTime = builder.endTime;
		this.duration = builder.duration;
		this.itemsProcessed = builder.itemsProcessed;
		this.itemsSucceeded = builder.itemsSucceeded;
		this.itemsFailed = builder.itemsFailed;
		this.errorMessage = builder.errorMessage;
		this.retryAttempt = builder.retryAttempt;
	}

	public boolean isSuccess() {
		return success;
	}

	public String getJobId() {
		return jobId;
	}

	public Instant getStartTime() {
		return startTime;
	}

	public Instant getEndTime() {
		return endTime;
	}

	public Duration getDuration() {
		return duration;
	}

	public int getItemsProcessed() {
		return itemsProcessed;
	}

	public int getItemsSucceeded() {
		return itemsSucceeded;
	}

	public int getItemsFailed() {
		return itemsFailed;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public int getRetryAttempt() {
		return retryAttempt;
	}

	public double getSuccessRate() {
		if (itemsProcessed == 0) {
			return 0.0;
		}
		return (itemsSucceeded * 100.0) / itemsProcessed;
	}

	public static Builder<?> builder() {
		return new Builder<>();
	}

	@SuppressWarnings("unchecked")
	public static class Builder<T extends Builder<T>> {

		protected boolean success;

		protected String jobId;

		protected Instant startTime;

		protected Instant endTime;

		protected Duration duration;

		protected int itemsProcessed;

		protected int itemsSucceeded;

		protected int itemsFailed;

		protected String errorMessage;

		protected int retryAttempt;

		public T success(boolean success) {
			this.success = success;
			return (T) this;
		}

		public T jobId(String jobId) {
			this.jobId = jobId;
			return (T) this;
		}

		public T startTime(Instant startTime) {
			this.startTime = startTime;
			return (T) this;
		}

		public T endTime(Instant endTime) {
			this.endTime = endTime;
			return (T) this;
		}

		public T duration(Duration duration) {
			this.duration = duration;
			return (T) this;
		}

		public T itemsProcessed(int itemsProcessed) {
			this.itemsProcessed = itemsProcessed;
			return (T) this;
		}

		public T itemsSucceeded(int itemsSucceeded) {
			this.itemsSucceeded = itemsSucceeded;
			return (T) this;
		}

		public T itemsFailed(int itemsFailed) {
			this.itemsFailed = itemsFailed;
			return (T) this;
		}

		public T errorMessage(String errorMessage) {
			this.errorMessage = errorMessage;
			return (T) this;
		}

		public T retryAttempt(int retryAttempt) {
			this.retryAttempt = retryAttempt;
			return (T) this;
		}

		public BatchJobResult build() {
			return new BatchJobResult(this);
		}

	}

}
