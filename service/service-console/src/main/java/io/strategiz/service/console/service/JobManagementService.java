package io.strategiz.service.console.service;

import io.strategiz.batch.marketdata.MarketDataBackfillJob;
import io.strategiz.batch.marketdata.MarketDataIncrementalJob;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.console.exception.ServiceConsoleErrorDetails;
import io.strategiz.service.console.model.response.JobResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing and monitoring scheduled jobs. Supports manual triggering from
 * console app.
 */
@Service
public class JobManagementService {

	private static final Logger logger = LoggerFactory.getLogger(JobManagementService.class);

	// Track job execution status
	private final Map<String, JobExecutionInfo> jobExecutions = new ConcurrentHashMap<>();

	// Job beans (optional - may not be present if scheduler profile not active)
	private final Optional<MarketDataBackfillJob> backfillJob;

	private final Optional<MarketDataIncrementalJob> incrementalJob;

	// Known jobs (registered at startup or manually added)
	private static final Map<String, JobConfig> KNOWN_JOBS = Map.of("marketDataBackfill",
			new JobConfig("marketDataBackfill", "Market Data Backfill",
					"Historical data backfill for all symbols across configured timeframes (1Day, 1Hour, 1Week, 1Month)",
					"Manual trigger only"),
			"marketDataIncremental",
			new JobConfig("marketDataIncremental", "Market Data Incremental",
					"Incremental collection of latest bars across all timeframes", "0 */5 * * * MON-FRI"));

	public JobManagementService(Optional<MarketDataBackfillJob> backfillJob,
			Optional<MarketDataIncrementalJob> incrementalJob) {
		this.backfillJob = backfillJob;
		this.incrementalJob = incrementalJob;

		logger.info("JobManagementService initialized: backfillJob={}, incrementalJob={}", backfillJob.isPresent(),
				incrementalJob.isPresent());
	}

	public List<JobResponse> listJobs() {
		logger.info("Listing all scheduled jobs");
		List<JobResponse> jobs = new ArrayList<>();

		for (Map.Entry<String, JobConfig> entry : KNOWN_JOBS.entrySet()) {
			JobConfig config = entry.getValue();
			JobResponse job = new JobResponse();
			job.setName(config.name);
			job.setDescription(config.description);
			job.setSchedule(config.schedule);
			job.setStatus("IDLE");

			// Check if job bean is available
			boolean beanAvailable = isJobBeanAvailable(config.name);
			job.setEnabled(beanAvailable);

			// Add execution info if available
			JobExecutionInfo execInfo = jobExecutions.get(config.name);
			if (execInfo != null) {
				job.setLastRunTime(execInfo.lastRunTime);
				job.setLastRunStatus(execInfo.status);
				job.setLastRunDurationMs(execInfo.durationMs);
				if (execInfo.running) {
					job.setStatus("RUNNING");
				}
			}

			jobs.add(job);
		}

		return jobs;
	}

	public JobResponse getJob(String jobName) {
		JobConfig config = KNOWN_JOBS.get(jobName);
		if (config == null) {
			throw new StrategizException(ServiceConsoleErrorDetails.JOB_NOT_FOUND, "service-console", jobName);
		}

		JobResponse job = new JobResponse();
		job.setName(config.name);
		job.setDescription(config.description);
		job.setSchedule(config.schedule);
		job.setStatus("IDLE");
		job.setEnabled(isJobBeanAvailable(config.name));

		JobExecutionInfo execInfo = jobExecutions.get(config.name);
		if (execInfo != null) {
			job.setLastRunTime(execInfo.lastRunTime);
			job.setLastRunStatus(execInfo.status);
			job.setLastRunDurationMs(execInfo.durationMs);
			if (execInfo.running) {
				job.setStatus("RUNNING");
			}
		}

		return job;
	}

	public JobResponse triggerJob(String jobName) {
		logger.info("Triggering job: {}", jobName);

		JobConfig config = KNOWN_JOBS.get(jobName);
		if (config == null) {
			throw new StrategizException(ServiceConsoleErrorDetails.JOB_NOT_FOUND, "service-console", jobName);
		}

		// Check if already running
		JobExecutionInfo existing = jobExecutions.get(jobName);
		if (existing != null && existing.running) {
			logger.warn("Job {} is already running", jobName);
			throw new StrategizException(ServiceConsoleErrorDetails.JOB_ALREADY_RUNNING, "service-console",
					jobName);
		}

		// Mark job as running
		JobExecutionInfo execInfo = new JobExecutionInfo();
		execInfo.running = true;
		execInfo.lastRunTime = Instant.now();
		execInfo.status = "RUNNING";
		jobExecutions.put(jobName, execInfo);

		// Execute the job synchronously
		executeJob(jobName);

		logger.info("Job {} has been triggered", jobName);

		return getJob(jobName);
	}

	/**
	 * Execute the job
	 */
	public void executeJob(String jobName) {
		long startTime = System.currentTimeMillis();

		try {
			switch (jobName) {
				case "marketDataBackfill":
					executeBackfillJob();
					break;
				case "marketDataIncremental":
					executeIncrementalJob();
					break;
				default:
					logger.warn("Unknown job: {}", jobName);
					recordJobCompletion(jobName, false, 0);
					return;
			}

			long duration = System.currentTimeMillis() - startTime;
			recordJobCompletion(jobName, true, duration);

		}
		catch (Exception e) {
			logger.error("Job {} failed: {}", jobName, e.getMessage(), e);
			long duration = System.currentTimeMillis() - startTime;
			recordJobCompletion(jobName, false, duration);
		}
	}

	private void executeBackfillJob() {
		if (backfillJob.isEmpty()) {
			logger.warn(
					"MarketDataBackfillJob bean not available. Start with scheduler profile or restart application.");
			throw new StrategizException(ServiceConsoleErrorDetails.JOB_NOT_AVAILABLE, "service-console",
					"Backfill job not available. Ensure scheduler profile is active or job bean is instantiated.");
		}

		if (backfillJob.get().isRunning()) {
			throw new StrategizException(ServiceConsoleErrorDetails.JOB_ALREADY_RUNNING, "service-console",
					"marketDataBackfill");
		}

		logger.info("Executing MarketDataBackfillJob.triggerManualExecution()");
		MarketDataBackfillJob.BackfillResult result = backfillJob.get().triggerManualExecution();

		if (!result.success) {
			throw new StrategizException(ServiceConsoleErrorDetails.JOB_EXECUTION_FAILED, "service-console",
					"Backfill failed: " + result.message);
		}

		logger.info("Backfill completed: {} symbols, {} data points, {} errors", result.symbolsProcessed,
				result.dataPointsStored, result.errorCount);
	}

	private void executeIncrementalJob() {
		if (incrementalJob.isEmpty()) {
			logger.warn(
					"MarketDataIncrementalJob bean not available. Start with scheduler profile or restart application.");
			throw new StrategizException(ServiceConsoleErrorDetails.JOB_NOT_AVAILABLE, "service-console",
					"Incremental job not available. Ensure scheduler profile is active or job bean is instantiated.");
		}

		if (incrementalJob.get().isRunning()) {
			throw new StrategizException(ServiceConsoleErrorDetails.JOB_ALREADY_RUNNING, "service-console",
					"marketDataIncremental");
		}

		logger.info("Executing MarketDataIncrementalJob.triggerManualExecution()");
		MarketDataIncrementalJob.IncrementalResult result = incrementalJob.get().triggerManualExecution();

		if (!result.success) {
			throw new StrategizException(ServiceConsoleErrorDetails.JOB_EXECUTION_FAILED, "service-console",
					"Incremental collection failed: " + result.message);
		}

		logger.info("Incremental completed: {} symbols, {} data points, {} errors", result.symbolsProcessed,
				result.dataPointsStored, result.errorCount);
	}

	private boolean isJobBeanAvailable(String jobName) {
		switch (jobName) {
			case "marketDataBackfill":
				return backfillJob.isPresent();
			case "marketDataIncremental":
				return incrementalJob.isPresent();
			default:
				return false;
		}
	}

	public void recordJobCompletion(String jobName, boolean success, long durationMs) {
		JobExecutionInfo execInfo = jobExecutions.computeIfAbsent(jobName, k -> new JobExecutionInfo());
		execInfo.running = false;
		execInfo.lastRunTime = Instant.now();
		execInfo.status = success ? "SUCCESS" : "FAILED";
		execInfo.durationMs = durationMs;
		logger.info("Job {} completed with status {} in {}ms", jobName, execInfo.status, durationMs);
	}

	// Internal classes
	private static class JobConfig {

		String name;

		String displayName;

		String description;

		String schedule;

		JobConfig(String name, String displayName, String description, String schedule) {
			this.name = name;
			this.displayName = displayName;
			this.description = description;
			this.schedule = schedule;
		}

	}

	private static class JobExecutionInfo {

		boolean running;

		Instant lastRunTime;

		String status;

		Long durationMs;

	}

}
