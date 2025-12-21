package io.strategiz.service.console.service;

import io.strategiz.service.console.model.response.JobResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;
import org.springframework.scheduling.config.ScheduledTask;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing and monitoring scheduled jobs.
 */
@Service
public class JobManagementService {

    private static final Logger logger = LoggerFactory.getLogger(JobManagementService.class);

    // Track job execution status
    private final Map<String, JobExecutionInfo> jobExecutions = new ConcurrentHashMap<>();

    // Known jobs (registered at startup or manually added)
    private static final Map<String, JobConfig> KNOWN_JOBS = Map.of(
        "marketDataCollection", new JobConfig(
            "marketDataCollection",
            "Multi-Timeframe Market Data Collection",
            "Collects OHLCV data for all symbols at 1Min, 1Hour, 1Day timeframes",
            "0 0 * * * *" // Every hour
        ),
        "symbolDiscovery", new JobConfig(
            "symbolDiscovery",
            "Symbol Discovery & Registration",
            "Discovers new tradeable symbols from exchanges",
            "0 0 6 * * *" // Daily at 6 AM
        )
    );

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
            throw new IllegalArgumentException("Job not found: " + jobName);
        }

        JobResponse job = new JobResponse();
        job.setName(config.name);
        job.setDescription(config.description);
        job.setSchedule(config.schedule);
        job.setStatus("IDLE");

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
            throw new IllegalArgumentException("Job not found: " + jobName);
        }

        // Mark job as running
        JobExecutionInfo execInfo = new JobExecutionInfo();
        execInfo.running = true;
        execInfo.lastRunTime = Instant.now();
        execInfo.status = "RUNNING";
        jobExecutions.put(jobName, execInfo);

        // TODO: Actually trigger the job via the scheduler or message queue
        logger.info("Job {} has been triggered", jobName);

        return getJob(jobName);
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
