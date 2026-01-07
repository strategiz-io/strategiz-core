package io.strategiz.batch.base;

import io.strategiz.batch.base.model.BatchJobResult;
import io.strategiz.batch.base.model.BatchJobStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Abstract base class for all batch jobs in Strategiz.
 *
 * Provides common functionality:
 * - Concurrent execution prevention (only one instance runs at a time)
 * - Execution timing and duration tracking
 * - Standardized result reporting
 * - Manual trigger support
 * - Status checking
 *
 * Subclasses implement {@link #doExecute()} with their specific logic.
 *
 * @param <T> The type of result returned by the job (extends BatchJobResult)
 */
public abstract class BaseBatchJob<T extends BatchJobResult> {

	protected final Logger log = LoggerFactory.getLogger(getClass());

	private final AtomicBoolean isRunning = new AtomicBoolean(false);

	private final AtomicReference<Instant> lastExecutionStart = new AtomicReference<>();

	private final AtomicReference<Instant> lastExecutionEnd = new AtomicReference<>();

	private final AtomicReference<T> lastResult = new AtomicReference<>();

	/**
	 * Get the unique job identifier. Used for logging and history tracking.
	 */
	public abstract String getJobId();

	/**
	 * Get human-readable job name.
	 */
	public abstract String getJobName();

	/**
	 * Check if this job is enabled via configuration.
	 */
	public abstract boolean isEnabled();

	/**
	 * Execute the job logic. Implemented by subclasses.
	 * @return The result of job execution
	 */
	protected abstract T doExecute();

	/**
	 * Create a result for when the job is disabled.
	 */
	protected abstract T createDisabledResult();

	/**
	 * Create a result for when the job is already running.
	 */
	protected abstract T createAlreadyRunningResult();

	/**
	 * Create a result for when the job fails with an exception.
	 */
	protected abstract T createFailedResult(Exception e, Duration duration);

	/**
	 * Execute the job with standard guards and tracking.
	 * This is the main entry point called by schedulers.
	 */
	public final T execute() {
		// Check if enabled
		if (!isEnabled()) {
			log.debug("{} is disabled, skipping execution", getJobName());
			return createDisabledResult();
		}

		// Prevent concurrent execution
		if (!isRunning.compareAndSet(false, true)) {
			log.warn("{} already running, skipping duplicate execution", getJobName());
			return createAlreadyRunningResult();
		}

		Instant startTime = Instant.now();
		lastExecutionStart.set(startTime);

		log.info("=== Starting {} ({}) ===", getJobName(), getJobId());

		try {
			T result = doExecute();

			Instant endTime = Instant.now();
			lastExecutionEnd.set(endTime);
			lastResult.set(result);

			Duration duration = Duration.between(startTime, endTime);
			log.info("=== {} completed in {}ms ===", getJobName(), duration.toMillis());

			return result;

		}
		catch (Exception e) {
			Instant endTime = Instant.now();
			lastExecutionEnd.set(endTime);
			Duration duration = Duration.between(startTime, endTime);

			log.error("=== {} failed after {}ms: {} ===", getJobName(), duration.toMillis(), e.getMessage(), e);

			T failedResult = createFailedResult(e, duration);
			lastResult.set(failedResult);
			return failedResult;

		}
		finally {
			isRunning.set(false);
		}
	}

	/**
	 * Manual trigger for console app or REST API. Logs the manual trigger source.
	 * @return The result of job execution
	 */
	public T triggerManualExecution() {
		log.info("Manual execution triggered for {}", getJobName());
		return execute();
	}

	/**
	 * Check if job is currently running.
	 */
	public boolean isRunning() {
		return isRunning.get();
	}

	/**
	 * Get current job status including execution state and timing.
	 */
	public BatchJobStatus getStatus() {
		return BatchJobStatus.builder()
			.jobId(getJobId())
			.jobName(getJobName())
			.enabled(isEnabled())
			.running(isRunning.get())
			.lastExecutionStart(lastExecutionStart.get())
			.lastExecutionEnd(lastExecutionEnd.get())
			.lastResult(lastResult.get())
			.build();
	}

	/**
	 * Get the last execution result.
	 */
	public T getLastResult() {
		return lastResult.get();
	}

	/**
	 * Get the duration of the last execution, or null if never run.
	 */
	public Duration getLastExecutionDuration() {
		Instant start = lastExecutionStart.get();
		Instant end = lastExecutionEnd.get();
		if (start != null && end != null) {
			return Duration.between(start, end);
		}
		return null;
	}

}
