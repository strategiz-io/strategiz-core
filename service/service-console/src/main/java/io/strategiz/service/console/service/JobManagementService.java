package io.strategiz.service.console.service;

import io.strategiz.batch.fundamentals.FundamentalsBackfillJob;
import io.strategiz.batch.fundamentals.FundamentalsIncrementalJob;
import io.strategiz.batch.marketdata.MarketDataBackfillJob;
import io.strategiz.batch.marketdata.MarketDataIncrementalJob;
import io.strategiz.business.fundamentals.model.CollectionResult;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.base.BaseService;
import io.strategiz.service.console.exception.ServiceConsoleErrorDetails;
import io.strategiz.service.console.model.response.JobResponse;
import org.slf4j.MDC;
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
public class JobManagementService extends BaseService {

	@Override
	protected String getModuleName() {
		return "service-console";
	}

	// Track job execution status
	private final Map<String, JobExecutionInfo> jobExecutions = new ConcurrentHashMap<>();

	// Job beans (optional - may not be present if scheduler profile not active)
	private final Optional<MarketDataBackfillJob> marketDataBackfillJob;

	private final Optional<MarketDataIncrementalJob> marketDataIncrementalJob;

	private final Optional<FundamentalsBackfillJob> fundamentalsBackfillJob;

	private final Optional<FundamentalsIncrementalJob> fundamentalsIncrementalJob;

	// Log streaming service for SSE
	private final JobLogStreamService jobLogStreamService;

	// Known jobs (registered at startup or manually added)
	private static final Map<String, JobConfig> KNOWN_JOBS = Map.of("marketDataBackfill",
			new JobConfig("marketDataBackfill", "Market Data Backfill",
					"Historical data backfill for all symbols across configured timeframes (1Day, 1Hour, 1Week, 1Month)",
					"Manual trigger only"),
			"marketDataIncremental",
			new JobConfig("marketDataIncremental", "Market Data Incremental",
					"Incremental collection of latest bars across all timeframes", "0 */5 * * * MON-FRI"),
			"fundamentalsBackfill",
			new JobConfig("fundamentalsBackfill", "Fundamentals Backfill",
					"Backfill company fundamentals data from Yahoo Finance for all configured symbols",
					"Manual trigger only"),
			"fundamentalsIncremental",
			new JobConfig("fundamentalsIncremental", "Fundamentals Incremental",
					"Daily update of company fundamentals data from Yahoo Finance", "0 0 2 * * *")
	);

	public JobManagementService(Optional<MarketDataBackfillJob> marketDataBackfillJob,
			Optional<MarketDataIncrementalJob> marketDataIncrementalJob,
			Optional<FundamentalsBackfillJob> fundamentalsBackfillJob,
			Optional<FundamentalsIncrementalJob> fundamentalsIncrementalJob, JobLogStreamService jobLogStreamService) {
		this.marketDataBackfillJob = marketDataBackfillJob;
		this.marketDataIncrementalJob = marketDataIncrementalJob;
		this.fundamentalsBackfillJob = fundamentalsBackfillJob;
		this.fundamentalsIncrementalJob = fundamentalsIncrementalJob;
		this.jobLogStreamService = jobLogStreamService;

		log.info(
				"JobManagementService initialized: marketDataBackfill={}, marketDataIncremental={}, fundamentalsBackfill={}, fundamentalsIncremental={}",
				marketDataBackfillJob.isPresent(), marketDataIncrementalJob.isPresent(),
				fundamentalsBackfillJob.isPresent(), fundamentalsIncrementalJob.isPresent());
	}

	public List<JobResponse> listJobs() {
		log.info("Listing all scheduled jobs");
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
		log.info("Triggering job: {}", jobName);

		JobConfig config = KNOWN_JOBS.get(jobName);
		if (config == null) {
			throw new StrategizException(ServiceConsoleErrorDetails.JOB_NOT_FOUND, "service-console", jobName);
		}

		// Check if already running
		JobExecutionInfo existing = jobExecutions.get(jobName);
		if (existing != null && existing.running) {
			log.warn("Job {} is already running", jobName);
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

		log.info("Job {} has been triggered", jobName);

		return getJob(jobName);
	}

	/**
	 * Execute the job
	 */
	public void executeJob(String jobName) {
		long startTime = System.currentTimeMillis();

		// CREATE LOG STREAM BEFORE JOB STARTS
		jobLogStreamService.createJobStream(jobName);

		// SET MDC CONTEXT FOR LOG FILTERING
		MDC.put("jobName", jobName);

		try {
			switch (jobName) {
				case "marketDataBackfill":
					executeMarketDataBackfillJob();
					break;
				case "marketDataIncremental":
					executeMarketDataIncrementalJob();
					break;
				case "fundamentalsBackfill":
					executeFundamentalsBackfillJob();
					break;
				case "fundamentalsIncremental":
					executeFundamentalsIncrementalJob();
					break;
				default:
					log.warn("Unknown job: {}", jobName);
					recordJobCompletion(jobName, false, 0);
					return;
			}

			long duration = System.currentTimeMillis() - startTime;
			recordJobCompletion(jobName, true, duration);

		}
		catch (Exception e) {
			log.error("Job {} failed: {}", jobName, e.getMessage(), e);
			long duration = System.currentTimeMillis() - startTime;
			recordJobCompletion(jobName, false, duration);
		}
		finally {
			// CLEANUP MDC
			MDC.remove("jobName");

			// SCHEDULE CLEANUP (after 5 min delay for late viewers)
			jobLogStreamService.scheduleJobCleanup(jobName);
		}
	}

	private void executeMarketDataBackfillJob() {
		if (marketDataBackfillJob.isEmpty()) {
			log.warn(
					"MarketDataBackfillJob bean not available. Start with scheduler profile or restart application.");
			throw new StrategizException(ServiceConsoleErrorDetails.JOB_NOT_AVAILABLE, "service-console",
					"Backfill job not available. Ensure scheduler profile is active or job bean is instantiated.");
		}

		if (marketDataBackfillJob.get().isRunning()) {
			throw new StrategizException(ServiceConsoleErrorDetails.JOB_ALREADY_RUNNING, "service-console",
					"marketDataBackfill");
		}

		log.info("Executing MarketDataBackfillJob.triggerManualExecution()");
		MarketDataBackfillJob.BackfillResult result = marketDataBackfillJob.get().triggerManualExecution();

		if (!result.success) {
			throw new StrategizException(ServiceConsoleErrorDetails.JOB_EXECUTION_FAILED, "service-console",
					"Backfill failed: " + result.message);
		}

		log.info("Backfill completed: {} symbols, {} data points, {} errors", result.symbolsProcessed,
				result.dataPointsStored, result.errorCount);
	}

	private void executeMarketDataIncrementalJob() {
		if (marketDataIncrementalJob.isEmpty()) {
			log.warn(
					"MarketDataIncrementalJob bean not available. Start with scheduler profile or restart application.");
			throw new StrategizException(ServiceConsoleErrorDetails.JOB_NOT_AVAILABLE, "service-console",
					"Incremental job not available. Ensure scheduler profile is active or job bean is instantiated.");
		}

		if (marketDataIncrementalJob.get().isRunning()) {
			throw new StrategizException(ServiceConsoleErrorDetails.JOB_ALREADY_RUNNING, "service-console",
					"marketDataIncremental");
		}

		log.info("Executing MarketDataIncrementalJob.triggerManualExecution()");
		MarketDataIncrementalJob.IncrementalResult result = marketDataIncrementalJob.get().triggerManualExecution();

		if (!result.success) {
			throw new StrategizException(ServiceConsoleErrorDetails.JOB_EXECUTION_FAILED, "service-console",
					"Incremental collection failed: " + result.message);
		}

		log.info("Incremental completed: {} symbols, {} data points, {} errors", result.symbolsProcessed,
				result.dataPointsStored, result.errorCount);
	}

	private void executeFundamentalsBackfillJob() {
		if (fundamentalsBackfillJob.isEmpty()) {
			log.warn(
					"FundamentalsBackfillJob bean not available. Start with scheduler profile or restart application.");
			throw new StrategizException(ServiceConsoleErrorDetails.JOB_NOT_AVAILABLE, "service-console",
					"Fundamentals backfill job not available. Ensure scheduler profile is active or job bean is instantiated.");
		}

		if (fundamentalsBackfillJob.get().isRunning()) {
			throw new StrategizException(ServiceConsoleErrorDetails.JOB_ALREADY_RUNNING, "service-console",
					"fundamentalsBackfill");
		}

		log.info("Executing FundamentalsBackfillJob.executeBackfill()");
		CollectionResult result = fundamentalsBackfillJob.get().executeBackfill();

		if (result.getErrorCount() > 0 && result.getSuccessCount() == 0) {
			throw new StrategizException(ServiceConsoleErrorDetails.JOB_EXECUTION_FAILED, "service-console",
					"Fundamentals backfill failed: all symbols failed");
		}

		log.info("Fundamentals backfill completed: {} symbols, {} success, {} errors", result.getTotalSymbols(),
				result.getSuccessCount(), result.getErrorCount());
	}

	private void executeFundamentalsIncrementalJob() {
		if (fundamentalsIncrementalJob.isEmpty()) {
			log.warn(
					"FundamentalsIncrementalJob bean not available. Start with scheduler profile or restart application.");
			throw new StrategizException(ServiceConsoleErrorDetails.JOB_NOT_AVAILABLE, "service-console",
					"Fundamentals incremental job not available. Ensure scheduler profile is active or job bean is instantiated.");
		}

		if (fundamentalsIncrementalJob.get().isRunning()) {
			throw new StrategizException(ServiceConsoleErrorDetails.JOB_ALREADY_RUNNING, "service-console",
					"fundamentalsIncremental");
		}

		log.info("Executing FundamentalsIncrementalJob.triggerManualExecution()");
		CollectionResult result = fundamentalsIncrementalJob.get().triggerManualExecution();

		if (result.getErrorCount() > 0 && result.getSuccessCount() == 0) {
			throw new StrategizException(ServiceConsoleErrorDetails.JOB_EXECUTION_FAILED, "service-console",
					"Fundamentals incremental failed: all symbols failed");
		}

		log.info("Fundamentals incremental completed: {} symbols, {} success, {} errors", result.getTotalSymbols(),
				result.getSuccessCount(), result.getErrorCount());
	}

	private boolean isJobBeanAvailable(String jobName) {
		switch (jobName) {
			case "marketDataBackfill":
				return marketDataBackfillJob.isPresent();
			case "marketDataIncremental":
				return marketDataIncrementalJob.isPresent();
			case "fundamentalsBackfill":
				return fundamentalsBackfillJob.isPresent();
			case "fundamentalsIncremental":
				return fundamentalsIncrementalJob.isPresent();
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
		log.info("Job {} completed with status {} in {}ms", jobName, execInfo.status, durationMs);
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
