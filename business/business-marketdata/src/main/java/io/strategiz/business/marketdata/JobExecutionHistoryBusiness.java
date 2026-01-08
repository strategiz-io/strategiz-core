package io.strategiz.business.marketdata;

import io.strategiz.business.marketdata.exception.MarketDataErrorDetails;
import io.strategiz.data.marketdata.timescale.entity.JobExecutionEntity;
import io.strategiz.data.marketdata.timescale.repository.JobExecutionRepository;
import io.strategiz.framework.exception.StrategizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Business service for tracking job execution history in TimescaleDB.
 *
 * Records job execution metadata for Market Data backfill and incremental jobs:
 * - Job start/completion timestamps
 * - Success/failure status
 * - Symbols processed and data points stored
 * - Error tracking for failed jobs
 *
 * Provides historical tracking and statistics:
 * - Execution history with pagination
 * - Success rate calculations
 * - Average duration trends
 * - Error frequency analysis
 */
@Service
public class JobExecutionHistoryBusiness {

    private static final Logger log = LoggerFactory.getLogger(JobExecutionHistoryBusiness.class);

    private final JobExecutionRepository jobExecutionRepository;

    @Autowired
    public JobExecutionHistoryBusiness(JobExecutionRepository jobExecutionRepository) {
        this.jobExecutionRepository = jobExecutionRepository;
    }

    /**
     * Record the start of a job execution.
     * Creates an execution record with status=RUNNING.
     * GRACEFUL: If database fails, returns a dummy ID and logs warning (job continues).
     *
     * @param jobId Job ID from jobs table (e.g., "MARKETDATA_INCREMENTAL")
     * @param jobName Legacy job name for backward compatibility
     * @param timeframes JSON array of timeframes being processed (e.g., ["1Day", "1Hour"])
     * @return The execution ID for tracking (may be dummy ID if DB fails)
     */
    public String recordJobStart(String jobId, String jobName, String timeframes) {
        if (jobName == null || jobName.trim().isEmpty()) {
            log.warn("jobName is null or empty, using 'UNKNOWN' for execution tracking");
            jobName = "UNKNOWN";
        }

        // Generate execution ID with timestamp
        String executionId = jobName + "_" + Instant.now().getEpochSecond();

        try {
            long startTime = System.currentTimeMillis();

            JobExecutionEntity entity = new JobExecutionEntity();
            entity.setExecutionId(executionId);
            entity.setJobId(jobId);  // Foreign key to jobs table
            entity.setJobName(jobName);
            entity.setStartTime(Instant.now());
            entity.setStatus("RUNNING");
            entity.setSymbolsProcessed(0);
            entity.setDataPointsStored(0L);
            entity.setErrorCount(0);
            entity.setTimeframes(timeframes);
            entity.setCreatedAt(Instant.now());

            jobExecutionRepository.save(entity);

            long duration = System.currentTimeMillis() - startTime;
            log.debug("Job execution start recorded in {}ms - jobId: {}, jobName: {}, executionId: {}",
                duration, jobId, jobName, executionId);

            log.info("Job execution started: {} (ID: {}, execution: {})", jobName, jobId, executionId);
        } catch (Exception e) {
            log.warn("Failed to record job start in database (job will continue without history tracking): {} - {}",
                e.getClass().getSimpleName(), e.getMessage());
        }
        return executionId;
    }

    /**
     * Record the completion of a job execution.
     * Updates the execution record with results and final status.
     * GRACEFUL: If database fails, logs warning and continues.
     *
     * @param executionId The execution ID from recordJobStart()
     * @param status Final status ("SUCCESS" or "FAILED")
     * @param symbolsProcessed Number of symbols processed
     * @param dataPointsStored Number of data points stored
     * @param errorCount Number of errors encountered
     * @param errorDetails JSON array of error details (optional)
     */
    public void recordJobCompletion(
            String executionId,
            String status,
            int symbolsProcessed,
            long dataPointsStored,
            int errorCount,
            String errorDetails) {

        if (executionId == null || executionId.trim().isEmpty()) {
            log.warn("executionId is null or empty, skipping completion recording");
            return;
        }
        if (status == null || status.trim().isEmpty()) {
            log.warn("status is null or empty, using 'UNKNOWN'");
            status = "UNKNOWN";
        }

        try {
            long startTime = System.currentTimeMillis();

            Optional<JobExecutionEntity> optional = jobExecutionRepository.findById(executionId);
            if (optional.isEmpty()) {
                log.warn("Job execution not found for completion: {}", executionId);
                return;
            }

            JobExecutionEntity entity = optional.get();
            Instant endTime = Instant.now();
            long durationMs = ChronoUnit.MILLIS.between(entity.getStartTime(), endTime);

            entity.setEndTime(endTime);
            entity.setDurationMs(durationMs);
            entity.setStatus(status);
            entity.setSymbolsProcessed(symbolsProcessed);
            entity.setDataPointsStored(dataPointsStored);
            entity.setErrorCount(errorCount);
            entity.setErrorDetails(errorDetails);

            jobExecutionRepository.save(entity);

            long operationDuration = System.currentTimeMillis() - startTime;
            log.debug("Job completion recorded in {}ms - executionId: {}, status: {}, jobDuration: {}ms",
                operationDuration, executionId, status, durationMs);

            log.info("Job execution completed: {} - Status: {}, Duration: {}ms, Symbols: {}, Data Points: {}",
                entity.getJobName(), status, durationMs, symbolsProcessed, dataPointsStored);
        } catch (Exception e) {
            log.warn("Failed to record job completion in database (results already processed): {} - {}",
                e.getClass().getSimpleName(), e.getMessage());
        }
    }

    /**
     * Get paginated execution history for a specific job.
     *
     * @param jobName Name of the job to query
     * @param page Page number (0-indexed)
     * @param pageSize Number of records per page
     * @return Page of execution records, ordered by start time descending
     */
    public Page<JobExecutionEntity> getExecutionHistory(String jobName, int page, int pageSize) {
        if (jobName == null || jobName.trim().isEmpty()) {
            throw new StrategizException(
                MarketDataErrorDetails.INVALID_INPUT,
                "business-marketdata",
                "jobName cannot be null or empty"
            );
        }

        long startTime = System.currentTimeMillis();

        Pageable pageable = PageRequest.of(page, pageSize);
        Page<JobExecutionEntity> executions = jobExecutionRepository.findByJobNameOrderByStartTimeDesc(jobName, pageable);

        long duration = System.currentTimeMillis() - startTime;
        log.debug("Retrieved {} execution records for job {} in {}ms (page {} of {})",
            executions.getNumberOfElements(), jobName, duration, page, executions.getTotalPages());

        return executions;
    }

    /**
     * Get execution statistics for a job within a time window.
     *
     * @param jobName Name of the job to analyze
     * @param sinceDays Number of days to look back (default 30)
     * @return Map containing: successCount, failureCount, successRate, avgDurationMs
     */
    public Map<String, Object> getExecutionStats(String jobName, int sinceDays) {
        if (jobName == null || jobName.trim().isEmpty()) {
            throw new StrategizException(
                MarketDataErrorDetails.INVALID_INPUT,
                "business-marketdata",
                "jobName cannot be null or empty"
            );
        }

        long startTime = System.currentTimeMillis();

        Instant since = Instant.now().minus(sinceDays, ChronoUnit.DAYS);

        // Get status distribution
        List<Object[]> statusCounts = jobExecutionRepository.getExecutionStatsByJobName(jobName, since);

        long successCount = 0;
        long failureCount = 0;
        for (Object[] row : statusCounts) {
            String status = (String) row[0];
            Long count = (Long) row[1];
            if ("SUCCESS".equals(status)) {
                successCount = count;
            } else if ("FAILED".equals(status)) {
                failureCount = count;
            }
        }

        long totalCount = successCount + failureCount;
        double successRate = (totalCount > 0) ? (double) successCount / totalCount * 100.0 : 0.0;

        // Get average duration for successful executions
        Long avgDuration = jobExecutionRepository.getAverageDuration(jobName, since);
        if (avgDuration == null) {
            avgDuration = 0L;
        }

        Map<String, Object> stats = Map.of(
            "successCount", successCount,
            "failureCount", failureCount,
            "totalCount", totalCount,
            "successRate", Math.round(successRate * 100.0) / 100.0, // Round to 2 decimals
            "avgDurationMs", avgDuration,
            "periodDays", sinceDays
        );

        long duration = System.currentTimeMillis() - startTime;
        log.debug("Execution stats for {} calculated in {}ms: {} total, {:.2f}% success",
            jobName, duration, totalCount, successRate);

        return stats;
    }

    /**
     * Get the most recent execution for a job.
     *
     * @param jobName Name of the job
     * @return Optional containing the most recent execution, or empty if none found
     */
    public Optional<JobExecutionEntity> getLatestExecution(String jobName) {
        if (jobName == null || jobName.trim().isEmpty()) {
            throw new StrategizException(
                MarketDataErrorDetails.INVALID_INPUT,
                "business-marketdata",
                "jobName cannot be null or empty"
            );
        }

        long startTime = System.currentTimeMillis();

        Optional<JobExecutionEntity> latest = jobExecutionRepository.findLatestByJobName(jobName);

        long duration = System.currentTimeMillis() - startTime;
        log.debug("Latest execution query for {} completed in {}ms, found: {}",
            jobName, duration, latest.isPresent());

        return latest;
    }

    /**
     * Get recent executions across all jobs within a time window.
     *
     * @param jobName Name of the job
     * @param sinceDays Number of days to look back
     * @return List of recent executions
     */
    public List<JobExecutionEntity> getRecentExecutions(String jobName, int sinceDays) {
        if (jobName == null || jobName.trim().isEmpty()) {
            throw new StrategizException(
                MarketDataErrorDetails.INVALID_INPUT,
                "business-marketdata",
                "jobName cannot be null or empty"
            );
        }

        long startTime = System.currentTimeMillis();

        Instant since = Instant.now().minus(sinceDays, ChronoUnit.DAYS);
        List<JobExecutionEntity> executions = jobExecutionRepository.findRecentExecutions(jobName, since);

        long duration = System.currentTimeMillis() - startTime;
        log.debug("Retrieved {} recent executions for job {} in {}ms (last {} days)",
            executions.size(), jobName, duration, sinceDays);

        return executions;
    }

    /**
     * Count executions by status for a job within a time window.
     *
     * @param jobName Name of the job
     * @param status Status to count ("SUCCESS", "FAILED", "RUNNING")
     * @param sinceDays Number of days to look back
     * @return Count of matching executions
     */
    public long countExecutionsByStatus(String jobName, String status, int sinceDays) {
        if (jobName == null || jobName.trim().isEmpty()) {
            throw new StrategizException(
                MarketDataErrorDetails.INVALID_INPUT,
                "business-marketdata",
                "jobName cannot be null or empty"
            );
        }
        if (status == null || status.trim().isEmpty()) {
            throw new StrategizException(
                MarketDataErrorDetails.INVALID_INPUT,
                "business-marketdata",
                "status cannot be null or empty"
            );
        }

        long startTime = System.currentTimeMillis();

        Instant since = Instant.now().minus(sinceDays, ChronoUnit.DAYS);
        Long count = jobExecutionRepository.countByJobNameAndStatusSince(jobName, status, since);

        long duration = System.currentTimeMillis() - startTime;
        log.debug("Count for job {} with status {} in {}ms: {} executions",
            jobName, status, duration, count);

        return count != null ? count : 0L;
    }

    /**
     * Get paginated execution history across ALL jobs.
     * Used by admin console to view complete job history.
     * Also cleans up stale RUNNING jobs (older than 12 hours) as a self-healing mechanism.
     *
     * @param pageable Pagination parameters
     * @return Page of execution records, ordered by start time descending
     */
    public Page<JobExecutionEntity> getAllExecutionHistory(Pageable pageable) {
        long startTime = System.currentTimeMillis();

        // Self-healing: clean up stale RUNNING jobs before returning results
        cleanupStaleRunningJobs();

        Page<JobExecutionEntity> executions = jobExecutionRepository.findAllOrderByStartTimeDesc(pageable);

        long duration = System.currentTimeMillis() - startTime;
        log.debug("Retrieved {} execution records across all jobs in {}ms (page {} of {})",
            executions.getNumberOfElements(), duration, pageable.getPageNumber(), executions.getTotalPages());

        return executions;
    }

    /**
     * Clean up stale RUNNING jobs by marking them as TIMEOUT.
     * Jobs stuck in RUNNING status for more than 12 hours are considered stale
     * (likely due to HTTP timeout or server restart).
     */
    public void cleanupStaleRunningJobs() {
        try {
            // Jobs running for more than 12 hours are considered stale
            Instant staleThreshold = Instant.now().minus(12, ChronoUnit.HOURS);
            List<JobExecutionEntity> staleJobs = jobExecutionRepository.findStaleRunningJobs(staleThreshold);

            if (!staleJobs.isEmpty()) {
                log.info("Found {} stale RUNNING jobs (started before {}), marking as TIMEOUT",
                    staleJobs.size(), staleThreshold);

                for (JobExecutionEntity job : staleJobs) {
                    job.setStatus("TIMEOUT");
                    job.setEndTime(Instant.now());
                    job.setErrorDetails("Job marked as TIMEOUT - likely HTTP request timeout or server restart");
                    jobExecutionRepository.save(job);
                    log.info("Marked stale job as TIMEOUT: {} (started {})", job.getJobName(), job.getStartTime());
                }
            }
        } catch (Exception e) {
            log.warn("Failed to clean up stale RUNNING jobs: {}", e.getMessage());
        }
    }

    /**
     * Get all executions across all jobs since a timestamp.
     * Used for calculating aggregate statistics in admin console.
     *
     * @param since Only return executions after this timestamp
     * @return List of execution records
     */
    public List<JobExecutionEntity> getAllExecutionsSince(Instant since) {
        if (since == null) {
            throw new StrategizException(
                MarketDataErrorDetails.INVALID_INPUT,
                "business-marketdata",
                "since cannot be null"
            );
        }

        long startTime = System.currentTimeMillis();

        List<JobExecutionEntity> executions = jobExecutionRepository.findAllSince(since);

        long duration = System.currentTimeMillis() - startTime;
        log.debug("Retrieved {} executions across all jobs since {} in {}ms",
            executions.size(), since, duration);

        return executions;
    }
}
