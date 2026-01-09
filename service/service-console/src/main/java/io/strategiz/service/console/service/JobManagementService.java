package io.strategiz.service.console.service;

import io.strategiz.batch.fundamentals.FundamentalsBackfillJob;
import io.strategiz.batch.fundamentals.FundamentalsIncrementalJob;
import io.strategiz.batch.livestrategies.DispatchJob;
import io.strategiz.batch.marketdata.MarketDataBackfillJob;
import io.strategiz.batch.marketdata.MarketDataIncrementalJob;
import io.strategiz.business.fundamentals.model.CollectionResult;
import io.strategiz.data.marketdata.firestore.entity.JobDefinitionFirestoreEntity;
import io.strategiz.data.marketdata.firestore.repository.JobDefinitionFirestoreRepository;
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

	private final Optional<DispatchJob> dispatchJob;

	// Log streaming service for SSE
	private final JobLogStreamService jobLogStreamService;

	// Job definitions repository (reads from Firestore)
	private final JobDefinitionFirestoreRepository jobDefinitionRepository;

	public JobManagementService(Optional<MarketDataBackfillJob> marketDataBackfillJob,
			Optional<MarketDataIncrementalJob> marketDataIncrementalJob,
			Optional<FundamentalsBackfillJob> fundamentalsBackfillJob,
			Optional<FundamentalsIncrementalJob> fundamentalsIncrementalJob,
			Optional<DispatchJob> dispatchJob,
			JobLogStreamService jobLogStreamService,
			JobDefinitionFirestoreRepository jobDefinitionRepository) {
		this.marketDataBackfillJob = marketDataBackfillJob;
		this.marketDataIncrementalJob = marketDataIncrementalJob;
		this.fundamentalsBackfillJob = fundamentalsBackfillJob;
		this.fundamentalsIncrementalJob = fundamentalsIncrementalJob;
		this.dispatchJob = dispatchJob;
		this.jobLogStreamService = jobLogStreamService;
		this.jobDefinitionRepository = jobDefinitionRepository;

		log.info(
				"JobManagementService initialized: marketDataBackfill={}, marketDataIncremental={}, fundamentalsBackfill={}, fundamentalsIncremental={}, dispatchJob={}",
				marketDataBackfillJob.isPresent(), marketDataIncrementalJob.isPresent(),
				fundamentalsBackfillJob.isPresent(), fundamentalsIncrementalJob.isPresent(),
				dispatchJob.isPresent());
	}

	public List<JobResponse> listJobs() {
		log.info("Listing all scheduled jobs from Firestore");
		List<JobResponse> jobs = new ArrayList<>();

		// Read job definitions from Firestore
		List<JobDefinitionFirestoreEntity> jobDefinitions = jobDefinitionRepository.findAllOrderByGroupAndName();

		for (JobDefinitionFirestoreEntity jobDef : jobDefinitions) {
			JobResponse job = new JobResponse();
			job.setName(jobDef.getJobId());
			job.setDisplayName(jobDef.getDisplayName());
			job.setDescription(jobDef.getDescription());
			job.setJobGroup(jobDef.getJobGroup());
			job.setScheduleType(jobDef.getScheduleType());

			// Set schedule display based on schedule type
			if ("CRON".equals(jobDef.getScheduleType())) {
				job.setSchedule(jobDef.getScheduleCron());
			}
			else {
				job.setSchedule("Manual trigger only");
			}

			job.setStatus("IDLE");

			// Check if job bean is available and enabled in database
			boolean beanAvailable = isJobBeanAvailable(jobDef.getJobId());
			job.setEnabled(jobDef.getEnabled() && beanAvailable);

			// Add execution info if available
			JobExecutionInfo execInfo = jobExecutions.get(jobDef.getJobId());
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

	public JobResponse getJob(String jobId) {
		// Read job definition from Firestore
		JobDefinitionFirestoreEntity jobDef = jobDefinitionRepository.findByJobId(jobId)
			.orElseThrow(() -> new StrategizException(ServiceConsoleErrorDetails.JOB_NOT_FOUND, "service-console",
					jobId));

		JobResponse job = new JobResponse();
		job.setName(jobDef.getJobId());
		job.setDisplayName(jobDef.getDisplayName());
		job.setDescription(jobDef.getDescription());
		job.setJobGroup(jobDef.getJobGroup());
		job.setScheduleType(jobDef.getScheduleType());

		// Set schedule display based on schedule type
		if ("CRON".equals(jobDef.getScheduleType())) {
			job.setSchedule(jobDef.getScheduleCron());
		}
		else {
			job.setSchedule("Manual trigger only");
		}

		job.setStatus("IDLE");

		// Check if job bean is available and enabled in database
		boolean beanAvailable = isJobBeanAvailable(jobDef.getJobId());
		job.setEnabled(jobDef.getEnabled() && beanAvailable);

		// Add execution info if available
		JobExecutionInfo execInfo = jobExecutions.get(jobDef.getJobId());
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

	public JobResponse triggerJob(String jobId) {
		log.info("Triggering job: {}", jobId);

		// Verify job exists in Firestore
		JobDefinitionFirestoreEntity jobDef = jobDefinitionRepository.findByJobId(jobId)
			.orElseThrow(() -> new StrategizException(ServiceConsoleErrorDetails.JOB_NOT_FOUND, "service-console",
					jobId));

		// Check if already running
		JobExecutionInfo existing = jobExecutions.get(jobId);
		if (existing != null && existing.running) {
			log.warn("Job {} is already running", jobId);
			throw new StrategizException(ServiceConsoleErrorDetails.JOB_ALREADY_RUNNING, "service-console", jobId);
		}

		// Mark job as running
		JobExecutionInfo execInfo = new JobExecutionInfo();
		execInfo.running = true;
		execInfo.lastRunTime = Instant.now();
		execInfo.status = "RUNNING";
		jobExecutions.put(jobId, execInfo);

		// Execute the job synchronously
		executeJob(jobId);

		log.info("Job {} has been triggered", jobId);

		return getJob(jobId);
	}

	/**
	 * Execute the job by job ID (e.g., MARKETDATA_BACKFILL)
	 */
	public void executeJob(String jobId) {
		long startTime = System.currentTimeMillis();

		// CREATE LOG STREAM BEFORE JOB STARTS
		jobLogStreamService.createJobStream(jobId);

		// SET MDC CONTEXT FOR LOG FILTERING
		MDC.put("jobName", jobId);

		try {
			switch (jobId) {
				case "MARKETDATA_BACKFILL":
					executeMarketDataBackfillJob();
					break;
				case "MARKETDATA_INCREMENTAL":
					executeMarketDataIncrementalJob();
					break;
				case "FUNDAMENTALS_BACKFILL":
					executeFundamentalsBackfillJob();
					break;
				case "FUNDAMENTALS_INCREMENTAL":
					executeFundamentalsIncrementalJob();
					break;
				case "DISPATCH_TIER1":
					executeDispatchJob("TIER1");
					break;
				case "DISPATCH_TIER2":
					executeDispatchJob("TIER2");
					break;
				case "DISPATCH_TIER3":
					executeDispatchJob("TIER3");
					break;
				default:
					log.warn("Unknown job: {}", jobId);
					recordJobCompletion(jobId, false, 0);
					return;
			}

			long duration = System.currentTimeMillis() - startTime;
			recordJobCompletion(jobId, true, duration);

		}
		catch (Exception e) {
			log.error("Job {} failed: {}", jobId, e.getMessage(), e);
			long duration = System.currentTimeMillis() - startTime;
			recordJobCompletion(jobId, false, duration);
		}
		finally {
			// CLEANUP MDC
			MDC.remove("jobName");

			// SCHEDULE CLEANUP (after 5 min delay for late viewers)
			jobLogStreamService.scheduleJobCleanup(jobId);
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

	private void executeDispatchJob(String tier) {
		if (dispatchJob.isEmpty()) {
			log.warn(
					"DispatchJob bean not available. Start with scheduler profile or restart application.");
			throw new StrategizException(ServiceConsoleErrorDetails.JOB_NOT_AVAILABLE, "service-console",
					"Live strategies dispatch job not available. Ensure scheduler profile is active.");
		}

		if (dispatchJob.get().isRunning()) {
			throw new StrategizException(ServiceConsoleErrorDetails.JOB_ALREADY_RUNNING, "service-console",
					"dispatch_" + tier);
		}

		log.info("Executing DispatchJob.triggerManualExecution({})", tier);
		DispatchJob.DispatchResult result = dispatchJob.get().triggerManualExecution(tier);

		if (!result.success()) {
			throw new StrategizException(ServiceConsoleErrorDetails.JOB_EXECUTION_FAILED, "service-console",
					"Dispatch failed for tier " + tier + ": " + result.errorMessage());
		}

		log.info("Dispatch {} completed: {} alerts, {} bots, {} symbol sets, {} messages published",
				tier, result.alertsProcessed(), result.botsProcessed(),
				result.symbolSetsCreated(), result.messagesPublished());
	}

	private boolean isJobBeanAvailable(String jobId) {
		switch (jobId) {
			case "MARKETDATA_BACKFILL":
				return marketDataBackfillJob.isPresent();
			case "MARKETDATA_INCREMENTAL":
				return marketDataIncrementalJob.isPresent();
			case "FUNDAMENTALS_BACKFILL":
				return fundamentalsBackfillJob.isPresent();
			case "FUNDAMENTALS_INCREMENTAL":
				return fundamentalsIncrementalJob.isPresent();
			case "DISPATCH_TIER1":
			case "DISPATCH_TIER2":
			case "DISPATCH_TIER3":
				return dispatchJob.isPresent();
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
	private static class JobExecutionInfo {

		boolean running;

		Instant lastRunTime;

		String status;

		Long durationMs;

	}

}
